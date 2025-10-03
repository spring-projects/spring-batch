/*
 * Copyright 2024-2025 the original author or authors.
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
package org.springframework.batch.core.repository.support;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;

/**
 * A {@link JobRepository} implementation that does not use or store batch meta-data. It
 * is intended for use-cases where restartability is not required and where the execution
 * context is not involved in any way (like sharing data between steps through the
 * execution context, or partitioned steps where partitions meta-data is shared between
 * the manager and workers through the execution context, etc). <br/>
 * This implementation holds a single job instance and a corresponding job execution that
 * are suitable for one-time jobs executed in their own JVM. This job repository works
 * with transactional steps as well as non-transactional steps (in which case, a
 * {@link ResourcelessTransactionManager} can be used). <br/>
 * This implementation is not thread-safe and should not be used in any concurrent
 * environment.
 *
 * @since 5.2.0
 * @author Mahmoud Ben Hassine
 */
public class ResourcelessJobRepository implements JobRepository {

	private @Nullable JobInstance jobInstance;

	private @Nullable JobExecution jobExecution;

	private long stepExecutionIdIncrementer = 0L;

	/*
	 * ===================================================================================
	 * Job operations
	 * ===================================================================================
	 */

	@Override
	public List<String> getJobNames() {
		if (this.jobInstance == null) {
			return Collections.emptyList();
		}
		return Collections.singletonList(this.jobInstance.getJobName());
	}

	/*
	 * ===================================================================================
	 * Job instance operations
	 * ===================================================================================
	 */

	@Override
	public List<JobInstance> getJobInstances(String jobName, int start, int count) {
		if (this.jobInstance == null) {
			return Collections.emptyList();
		}
		return Collections.singletonList(this.jobInstance);
	}

	/**
	 * Find all {@link JobInstance}s for a given job name. In this implementation, only
	 * one job instance is held, so if it is initialized, it is returned in a single-item
	 * list.
	 * @param jobName The name of the job to query.
	 * @return a list of {@link JobInstance}s for the given job name.
	 */
	@Override
	public List<JobInstance> findJobInstances(String jobName) {
		if (this.jobInstance == null) {
			return Collections.emptyList();
		}
		return Collections.singletonList(this.jobInstance);
	}

	@Override
	@Nullable public JobInstance getJobInstance(long instanceId) {
		return this.jobInstance;
	}

	@Override
	@Nullable public JobInstance getLastJobInstance(String jobName) {
		return this.jobInstance;
	}

	@Override
	@Nullable public JobInstance getJobInstance(String jobName, JobParameters jobParameters) {
		return this.jobInstance;
	}

	@SuppressWarnings("removal")
	@Override
	@Deprecated(since = "6.0", forRemoval = true)
	public boolean isJobInstanceExists(String jobName, JobParameters jobParameters) {
		return false;
	}

	@Override
	public long getJobInstanceCount(String jobName) {
		// FIXME should return 0 if jobInstance is null or the name is not matching
		return 1;
	}

	@Override
	public JobInstance createJobInstance(String jobName, JobParameters jobParameters) {
		this.jobInstance = new JobInstance(1L, jobName);
		return this.jobInstance;
	}

	/*
	 * ===================================================================================
	 * Job execution operations
	 * ===================================================================================
	 */

	@Override
	@Nullable public JobExecution getJobExecution(long executionId) {
		// FIXME should return null if the id is not matching
		return this.jobExecution;
	}

	@Override
	@Nullable public JobExecution getLastJobExecution(String jobName, JobParameters jobParameters) {
		// FIXME should return null if the job name is not matching
		return this.jobExecution;
	}

	@Override
	@Nullable public JobExecution getLastJobExecution(JobInstance jobInstance) {
		// FIXME should return null if the job instance is not matching
		return this.jobExecution;
	}

	@Override
	public List<JobExecution> getJobExecutions(JobInstance jobInstance) {
		if (this.jobExecution == null) {
			return Collections.emptyList();
		}
		return Collections.singletonList(this.jobExecution);
	}

	@Override
	public JobExecution createJobExecution(JobInstance jobInstance, JobParameters jobParameters,
			ExecutionContext executionContext) {
		if (this.jobInstance == null || !(this.jobInstance.getId() == jobInstance.getId())) {
			throw new IllegalStateException(
					"The job instance passed as a parameter is not recognized by this job repository");
		}
		this.jobExecution = new JobExecution(1L, this.jobInstance, jobParameters);
		this.jobExecution.setExecutionContext(executionContext);
		this.jobInstance.addJobExecution(this.jobExecution);
		return this.jobExecution;
	}

	@Override
	public void update(JobExecution jobExecution) {
		jobExecution.setLastUpdated(LocalDateTime.now());
		this.jobExecution = jobExecution;
	}

	@Override
	public void updateExecutionContext(JobExecution jobExecution) {
		jobExecution.setLastUpdated(LocalDateTime.now());
	}

	/*
	 * ===================================================================================
	 * Step execution operations
	 * ===================================================================================
	 */

	@Override
	public StepExecution createStepExecution(String stepName, JobExecution jobExecution) {
		StepExecution stepExecution = new StepExecution(++stepExecutionIdIncrementer, stepName, jobExecution);
		stepExecution.setStartTime(LocalDateTime.now());
		stepExecution.setStatus(BatchStatus.STARTING);
		stepExecution.setLastUpdated(LocalDateTime.now());
		stepExecution.incrementVersion();
		jobExecution.addStepExecution(stepExecution);
		return stepExecution;
	}

	@Deprecated(since = "6.0", forRemoval = true)
	@Override
	@Nullable public StepExecution getStepExecution(long jobExecutionId, long stepExecutionId) {
		if (this.jobExecution == null || !(this.jobExecution.getId() == jobExecutionId)) {
			return null;
		}
		return this.jobExecution.getStepExecutions()
			.stream()
			.filter(stepExecution -> stepExecution.getId() == stepExecutionId)
			.findFirst()
			.orElse(null);
	}

	/**
	 * Retrieve a {@link StepExecution} by its id.
	 * @param stepExecutionId the id of the step execution to retrieve
	 * @return the {@link StepExecution} with the given id if it exists, null otherwise.
	 * @since 6.0
	 */
	@Override
	@Nullable public StepExecution getStepExecution(long stepExecutionId) {
		if (this.jobExecution == null) {
			return null;
		}
		return this.jobExecution.getStepExecutions()
			.stream()
			.filter(stepExecution -> stepExecution.getId() == stepExecutionId)
			.findFirst()
			.orElse(null);
	}

	@Override
	@Nullable public StepExecution getLastStepExecution(JobInstance jobInstance, String stepName) {
		if (this.jobExecution == null || !(this.jobExecution.getJobInstance().getId() == jobInstance.getId())) {
			return null;
		}
		return this.jobExecution.getStepExecutions()
			.stream()
			.filter(stepExecution -> stepExecution.getStepName().equals(stepName))
			.findFirst()
			.orElse(null);
	}

	@Override
	public long getStepExecutionCount(JobInstance jobInstance, String stepName) {
		if (this.jobExecution == null || !(this.jobExecution.getJobInstance().getId() == jobInstance.getId())) {
			throw new IllegalStateException(
					"The job instance passed as a parameter is not recognized by this job repository");
		}
		return this.jobExecution.getStepExecutions()
			.stream()
			.filter(stepExecution -> stepExecution.getStepName().equals(stepName))
			.count();
	}

	@Override
	public void update(StepExecution stepExecution) {
		stepExecution.setLastUpdated(LocalDateTime.now());
		if (this.jobExecution != null && this.jobExecution.isStopping()) {
			stepExecution.setTerminateOnly();
		}
	}

	@Override
	public void updateExecutionContext(StepExecution stepExecution) {
		stepExecution.setLastUpdated(LocalDateTime.now());
	}

}