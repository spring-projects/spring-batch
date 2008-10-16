package org.springframework.batch.core.partition.support;

import java.util.HashSet;
import java.util.Map;
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

public class SimpleStepExecutionSplitter implements StepExecutionSplitter {

	private static final String STEP_NAME_SEPARATOR = ":";

	private final String stepName;

	private final Partitioner partitioner;

	private final Step step;

	private final JobRepository jobRepository;

	public SimpleStepExecutionSplitter(JobRepository jobRepository, Step step) {
		this(jobRepository, step, new SimplePartitioner());
	}

	public SimpleStepExecutionSplitter(JobRepository jobRepository, Step step, Partitioner partitioner) {
		this.jobRepository = jobRepository;
		this.step = step;
		this.partitioner = partitioner;
		this.stepName = step.getName();
	}

	/**
	 * @see StepExecutionSplitter#getStepName()
	 */
	public String getStepName() {
		return this.stepName;
	}

	/**
	 * @see StepExecutionSplitter#split(StepExecution, int)
	 */
	public Set<StepExecution> split(StepExecution stepExecution, int gridSize) throws JobExecutionException {

		JobExecution jobExecution = stepExecution.getJobExecution();

		// If this is a restart we must retain the same grid size, ignoring the
		// one passed in...
		int splitSize = getSplitSize(stepExecution, gridSize);

		Map<String, ExecutionContext> contexts = partitioner.partition(splitSize);
		Set<StepExecution> set = new HashSet<StepExecution>(contexts.size());

		for (String key : contexts.keySet()) {

			// Make the step execution name unique and repeatable
			String stepName = this.stepName + STEP_NAME_SEPARATOR + key;

			StepExecution currentStepExecution = jobExecution.createStepExecution(stepName);

			boolean startable = getStartable(currentStepExecution, contexts.get(key));

			if (startable) {
				set.add(currentStepExecution);
			}

		}

		return set;

	}

	private int getSplitSize(StepExecution stepExecution, int gridSize) {
		ExecutionContext context = stepExecution.getExecutionContext();
		int result = (int) context.getLong("GRID_SIZE", gridSize);
		context.putLong("GRID_SIZE", result);
		if (context.isDirty()) {
			jobRepository.updateExecutionContext(stepExecution);
		}
		return result;
	}

	private boolean getStartable(StepExecution stepExecution, ExecutionContext context) throws JobExecutionException {

		JobInstance jobInstance = stepExecution.getJobExecution().getJobInstance();
		String stepName = stepExecution.getStepName();
		StepExecution lastStepExecution = jobRepository.getLastStepExecution(jobInstance, stepName);

		boolean isRestart = (lastStepExecution != null && lastStepExecution.getStatus() != BatchStatus.COMPLETED) ? true
				: false;
		
		if (isRestart) {
			stepExecution.setExecutionContext(lastStepExecution.getExecutionContext());
		}
		else {
			stepExecution.setExecutionContext(context);
		}

		return shouldStart(step, lastStepExecution) || isRestart;

	}

	private boolean shouldStart(Step step, StepExecution lastStepExecution) throws JobExecutionException {

		if (lastStepExecution == null) {
			return true;
		}

		BatchStatus stepStatus = lastStepExecution.getStatus();

		if (stepStatus == BatchStatus.UNKNOWN) {
			throw new JobExecutionException("Cannot restart step from UNKNOWN status.  "
					+ "The last execution ended with a failure that could not be rolled back, "
					+ "so it may be dangerous to proceed.  " + "Manual intervention is probably necessary.");
		}

		if (stepStatus == BatchStatus.COMPLETED && step.isAllowStartIfComplete() == false) {
			// step is complete, false should be returned, indicating that the
			// step should not be started
			return false;
		}

		return true;

	}

}
