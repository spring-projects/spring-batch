/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.core.partition.support;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Convenience class for aggregating a set of {@link StepExecution} instances
 * when the input comes from remote steps, so the data need to be refreshed from
 * the repository.
 * 
 * @author Dave Syer
 * @since 2.1
 */
public class RemoteStepExecutionAggregator implements StepExecutionAggregator, InitializingBean {

	private StepExecutionAggregator delegate = new DefaultStepExecutionAggregator();

	private JobExplorer jobExplorer;

	/**
	 * Create a new instance (useful for configuration purposes).
	 */
	public RemoteStepExecutionAggregator() {
	}

	/**
	 * Create a new instance with a job explorer that can be used to refresh the
	 * data when aggregating.
	 * 
	 * @param jobExplorer the {@link JobExplorer} to use
	 */
	public RemoteStepExecutionAggregator(JobExplorer jobExplorer) {
		super();
		this.jobExplorer = jobExplorer;
	}

	/**
	 * @param jobExplorer the jobExplorer to set
	 */
	public void setJobExplorer(JobExplorer jobExplorer) {
		this.jobExplorer = jobExplorer;
	}

	/**
	 * @param delegate the delegate to set
	 */
	public void setDelegate(StepExecutionAggregator delegate) {
		this.delegate = delegate;
	}

	/**
	 * @throws Exception if the job explorer is not provided
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.state(jobExplorer != null, "A JobExplorer must be provided");
	}

	/**
	 * Aggregates the input executions into the result {@link StepExecution}
	 * delegating to the delegate aggregator once the input has been refreshed
	 * from the {@link JobExplorer}.
	 * 
	 * @see StepExecutionAggregator #aggregate(StepExecution, Collection)
	 */
	public void aggregate(StepExecution result, Collection<StepExecution> executions) {
		Assert.notNull(result, "To aggregate into a result it must be non-null.");
		if (executions == null) {
			return;
		}
		Collection<StepExecution> updates = new ArrayList<StepExecution>();
		for (StepExecution stepExecution : executions) {
			Long id = stepExecution.getId();
			Assert.state(id != null, "StepExecution has null id. It must be saved first: " + stepExecution);
			StepExecution update = jobExplorer.getStepExecution(stepExecution.getJobExecutionId(), id);
			Assert.state(update != null, "Could not reload StepExecution from JobRepository: " + stepExecution);
			updates.add(update);
		}
		delegate.aggregate(result, updates);
	}

}
