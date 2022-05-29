/*
 * Copyright 2006-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.core.partition.support;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.StepExecutionSplitter;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Generic implementation of {@link StepExecutionSplitter} that delegates to a
 * {@link Partitioner} to generate {@link ExecutionContext} instances. Takes care of
 * restartability and identifying the step executions from previous runs of the same job.
 * The generated {@link StepExecution} instances have names that identify them uniquely in
 * the partition. The name is constructed from a base (name of the target step) plus a
 * suffix taken from the {@link Partitioner} identifiers, separated by a colon, e.g.
 * <code>{step1:partition0, step1:partition1, ...}</code>.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @since 2.0
 */
public class SimpleStepExecutionSplitter implements StepExecutionSplitter, InitializingBean {

	private static final String STEP_NAME_SEPARATOR = ":";

	private String stepName;

	private Partitioner partitioner;

	private boolean allowStartIfComplete = false;

	private JobRepository jobRepository;

	/**
	 * Default constructor for convenience in configuration.
	 */
	public SimpleStepExecutionSplitter() {
	}

	/**
	 * Construct a {@link SimpleStepExecutionSplitter} from its mandatory properties.
	 * @param jobRepository the {@link JobRepository}
	 * @param allowStartIfComplete flag specifying preferences on restart
	 * @param stepName the target step name
	 * @param partitioner a {@link Partitioner} to use for generating input parameters
	 */
	public SimpleStepExecutionSplitter(JobRepository jobRepository, boolean allowStartIfComplete, String stepName,
			Partitioner partitioner) {
		this.jobRepository = jobRepository;
		this.allowStartIfComplete = allowStartIfComplete;
		this.partitioner = partitioner;
		this.stepName = stepName;
	}

	/**
	 * Check mandatory properties (step name, job repository and partitioner).
	 *
	 * @see InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(jobRepository != null, "A JobRepository is required");
		Assert.state(stepName != null, "A step name is required");
		Assert.state(partitioner != null, "A Partitioner is required");
	}

	/**
	 * Flag to indicate that the partition target step is allowed to start if an execution
	 * is complete. Defaults to the same value as the underlying step. Set this manually
	 * to override the underlying step properties.
	 *
	 * @see Step#isAllowStartIfComplete()
	 * @param allowStartIfComplete the value to set
	 */
	public void setAllowStartIfComplete(boolean allowStartIfComplete) {
		this.allowStartIfComplete = allowStartIfComplete;
	}

	/**
	 * The job repository that will be used to manage the persistence of the delegate step
	 * executions.
	 * @param jobRepository the JobRepository to set
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * The {@link Partitioner} that will be used to generate step execution meta data for
	 * the target step.
	 * @param partitioner the partitioner to set
	 */
	public void setPartitioner(Partitioner partitioner) {
		this.partitioner = partitioner;
	}

	/**
	 * The name of the target step that will be executed across the partitions. Mandatory
	 * with no default.
	 * @param stepName the step name to set
	 */
	public void setStepName(String stepName) {
		this.stepName = stepName;
	}

	/**
	 * @see StepExecutionSplitter#getStepName()
	 */
	@Override
	public String getStepName() {
		return this.stepName;
	}

	/**
	 * @see StepExecutionSplitter#split(StepExecution, int)
	 */
	@Override
	public Set<StepExecution> split(StepExecution stepExecution, int gridSize) throws JobExecutionException {

		JobExecution jobExecution = stepExecution.getJobExecution();

		Map<String, ExecutionContext> contexts = getContexts(stepExecution, gridSize);
		Set<StepExecution> set = new HashSet<>(contexts.size());

		for (Entry<String, ExecutionContext> context : contexts.entrySet()) {

			// Make the step execution name unique and repeatable
			String stepName = this.stepName + STEP_NAME_SEPARATOR + context.getKey();

			StepExecution currentStepExecution = jobExecution.createStepExecution(stepName);

			boolean startable = isStartable(currentStepExecution, context.getValue());

			if (startable) {
				set.add(currentStepExecution);
			}
		}

		jobRepository.addAll(set);

		Set<StepExecution> executions = new HashSet<>(set.size());
		executions.addAll(set);

		return executions;

	}

	private Map<String, ExecutionContext> getContexts(StepExecution stepExecution, int gridSize) {

		ExecutionContext context = stepExecution.getExecutionContext();
		String key = SimpleStepExecutionSplitter.class.getSimpleName() + ".GRID_SIZE";

		// If this is a restart we must retain the same grid size, ignoring the
		// one passed in...
		int splitSize = (int) context.getLong(key, gridSize);
		context.putLong(key, splitSize);

		Map<String, ExecutionContext> result;
		if (context.isDirty()) {
			// The context changed so we didn't already know the partitions
			jobRepository.updateExecutionContext(stepExecution);
			result = partitioner.partition(splitSize);
		}
		else {
			if (partitioner instanceof PartitionNameProvider) {
				result = new HashMap<>();
				Collection<String> names = ((PartitionNameProvider) partitioner).getPartitionNames(splitSize);
				for (String name : names) {
					/*
					 * We need to return the same keys as the original (failed) execution,
					 * but the execution contexts will be discarded so they can be empty.
					 */
					result.put(name, new ExecutionContext());
				}
			}
			else {
				// If no names are provided, grab the partition again.
				result = partitioner.partition(splitSize);
			}
		}

		return result;
	}

	/**
	 * Check if a step execution is startable.
	 * @param stepExecution the step execution to check
	 * @param context the execution context of the step
	 * @return true if the step execution is startable, false otherwise
	 * @throws JobExecutionException if unable to check if the step execution is startable
	 */
	protected boolean isStartable(StepExecution stepExecution, ExecutionContext context) throws JobExecutionException {
		JobInstance jobInstance = stepExecution.getJobExecution().getJobInstance();
		String stepName = stepExecution.getStepName();
		StepExecution lastStepExecution = jobRepository.getLastStepExecution(jobInstance, stepName);

		boolean isRestart = (lastStepExecution != null && lastStepExecution.getStatus() != BatchStatus.COMPLETED);

		if (isRestart) {
			stepExecution.setExecutionContext(lastStepExecution.getExecutionContext());
		}
		else {
			stepExecution.setExecutionContext(context);
		}

		return shouldStart(allowStartIfComplete, stepExecution, lastStepExecution) || isRestart;

	}

	private boolean shouldStart(boolean allowStartIfComplete, StepExecution stepExecution,
			StepExecution lastStepExecution) throws JobExecutionException {

		if (lastStepExecution == null) {
			return true;
		}

		BatchStatus stepStatus = lastStepExecution.getStatus();

		if (stepStatus == BatchStatus.UNKNOWN) {
			throw new JobExecutionException("Cannot restart step from UNKNOWN status.  "
					+ "The last execution ended with a failure that could not be rolled back, "
					+ "so it may be dangerous to proceed.  " + "Manual intervention is probably necessary.");
		}

		if (stepStatus == BatchStatus.COMPLETED) {
			if (!allowStartIfComplete) {
				if (isSameJobExecution(stepExecution, lastStepExecution)) {
					// it's always OK to start again in the same JobExecution
					return true;
				}
				// step is complete, false should be returned, indicating that
				// the step should not be started
				return false;
			}
			else {
				return true;
			}
		}

		if (stepStatus == BatchStatus.STOPPED || stepStatus == BatchStatus.FAILED) {
			return true;
		}

		if (stepStatus == BatchStatus.STARTED || stepStatus == BatchStatus.STARTING
				|| stepStatus == BatchStatus.STOPPING) {
			throw new JobExecutionException("Cannot restart step from " + stepStatus + " status.  "
					+ "The old execution may still be executing, so you may need to verify manually that this is the case.");
		}

		throw new JobExecutionException("Cannot restart step from " + stepStatus + " status.  "
				+ "We believe the old execution was abandoned and therefore has been marked as un-restartable.");

	}

	private boolean isSameJobExecution(StepExecution stepExecution, StepExecution lastStepExecution) {
		if (stepExecution.getJobExecutionId() == null) {
			return lastStepExecution.getJobExecutionId() == null;
		}
		return stepExecution.getJobExecutionId().equals(lastStepExecution.getJobExecutionId());
	}

}
