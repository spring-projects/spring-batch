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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
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

	private JobInstance jobInstance;

	private JobExecution jobExecution;

	@Override
	public boolean isJobInstanceExists(String jobName, JobParameters jobParameters) {
		return false;
	}

	@Override
	public long getJobInstanceCount(String jobName) {
		return 1;
	}

	@Override
	public JobInstance createJobInstance(String jobName, JobParameters jobParameters) {
		this.jobInstance = new JobInstance(1L, jobName);
		return this.jobInstance;
	}

	@Override
	public JobExecution createJobExecution(String jobName, JobParameters jobParameters) {
		if (this.jobInstance == null) {
			createJobInstance(jobName, jobParameters);
		}
		this.jobExecution = new JobExecution(this.jobInstance, 1L, jobParameters);
		return this.jobExecution;
	}

	@Override
	public void update(JobExecution jobExecution) {
		jobExecution.setLastUpdated(LocalDateTime.now());
		this.jobExecution = jobExecution;
	}

	@Override
	public void add(StepExecution stepExecution) {
		this.addAll(Collections.singletonList(stepExecution));
	}

	@Override
	public void addAll(Collection<StepExecution> stepExecutions) {
		this.jobExecution.addStepExecutions(new ArrayList<>(stepExecutions));
	}

	@Override
	public void update(StepExecution stepExecution) {
		stepExecution.setLastUpdated(LocalDateTime.now());
		if (this.jobExecution.isStopping()) {
			stepExecution.setTerminateOnly();
		}
	}

	@Override
	public void updateExecutionContext(StepExecution stepExecution) {
		stepExecution.setLastUpdated(LocalDateTime.now());
	}

	@Override
	public void updateExecutionContext(JobExecution jobExecution) {
		jobExecution.setLastUpdated(LocalDateTime.now());
	}

	@Override
	public StepExecution getLastStepExecution(JobInstance jobInstance, String stepName) {
		return this.jobExecution.getStepExecutions()
			.stream()
			.filter(stepExecution -> stepExecution.getStepName().equals(stepName))
			.findFirst()
			.orElse(null);
	}

	@Override
	public long getStepExecutionCount(JobInstance jobInstance, String stepName) {
		return this.jobExecution.getStepExecutions()
			.stream()
			.filter(stepExecution -> stepExecution.getStepName().equals(stepName))
			.count();
	}

	@Override
	public JobExecution getLastJobExecution(String jobName, JobParameters jobParameters) {
		return this.jobExecution;
	}

}