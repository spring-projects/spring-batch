/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.job.flow;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.job.SimpleStepHandler;
import org.springframework.batch.core.job.flow.FlowExecutionException;
import org.springframework.batch.core.job.flow.FlowJob;
import org.springframework.batch.core.job.flow.JobFlowExecutor;

/**
 * JSR-352 specific extension of the {@link FlowJob}.
 *
 * @author Michael Minella
 * @since 3.0
 */
public class JsrFlowJob extends FlowJob {

	/**
	 * No arg constructor (invalid state)
	 */
	public JsrFlowJob() {
		super();
	}

	/**
	 * Main constructor
	 *
	 * @param name of the flow
	 */
	public JsrFlowJob(String name) {
		super(name);
	}

	/**
	 * @see AbstractJob#doExecute(JobExecution)
	 */
	@Override
	protected void doExecute(final JobExecution execution) throws JobExecutionException {
		try {
			JobFlowExecutor executor = new JsrFlowExecutor(getJobRepository(),
					new SimpleStepHandler(getJobRepository()), execution);
			executor.updateJobExecutionStatus(flow.start(executor).getStatus());
		}
		catch (FlowExecutionException e) {
			if (e.getCause() instanceof JobExecutionException) {
				throw (JobExecutionException) e.getCause();
			}
			throw new JobExecutionException("Flow execution ended unexpectedly", e);
		}
	}
}
