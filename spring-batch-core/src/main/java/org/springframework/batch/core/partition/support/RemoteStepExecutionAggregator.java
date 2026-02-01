/*
 * Copyright 2006-2026 the original author or authors.
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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.partition.StepExecutionAggregator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.util.Assert;

/**
 * Convenience class for aggregating a set of {@link StepExecution} instances when the
 * input comes from remote steps, so the data need to be refreshed from the repository.
 *
 * @author Dave Syer
 * @since 2.1
 */
public class RemoteStepExecutionAggregator implements StepExecutionAggregator {

	private StepExecutionAggregator delegate = new DefaultStepExecutionAggregator();

	private JobRepository jobRepository;

	/**
	 * Create a new instance with a job repository that can be used to refresh the data
	 * when aggregating.
	 * @param jobRepository the {@link JobRepository} to use
	 */
	public RemoteStepExecutionAggregator(JobRepository jobRepository) {
		super();
		this.jobRepository = jobRepository;
	}

	/**
	 * @param jobRepository the jobRepository to set
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * @param delegate the delegate to set
	 */
	public void setDelegate(StepExecutionAggregator delegate) {
		this.delegate = delegate;
	}

	/**
	 * Aggregates the input executions into the result {@link StepExecution} delegating to
	 * the delegate aggregator once the input has been refreshed from the
	 * {@link JobRepository}.
	 *
	 * @see StepExecutionAggregator #aggregate(StepExecution, Collection)
	 */
	@Override
	public void aggregate(StepExecution result, Collection<StepExecution> executions) {
		Assert.notNull(result, "To aggregate into a result it must be non-null.");
		if (executions == null) {
			return;
		}
		Set<Long> stepExecutionIds = executions.stream().map(stepExecution -> {
			long id = stepExecution.getId();
			return id;
		}).collect(Collectors.toSet());
		JobExecution jobExecution = jobRepository.getJobExecution(result.getJobExecutionId());
		Assert.state(jobExecution != null,
				"Could not load JobExecution from JobRepository for id " + result.getJobExecutionId());
		List<StepExecution> updates = jobExecution.getStepExecutions()
			.stream()
			.filter(stepExecution -> stepExecutionIds.contains(stepExecution.getId()))
			.collect(Collectors.toList());
		delegate.aggregate(result, updates);
	}

}
