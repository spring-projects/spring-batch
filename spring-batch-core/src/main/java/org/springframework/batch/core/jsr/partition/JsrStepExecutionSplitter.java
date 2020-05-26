/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.batch.core.jsr.partition;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.jsr.launch.JsrJobOperator;
import org.springframework.batch.core.partition.support.SimpleStepExecutionSplitter;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ExecutionContext;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * Provides JSR-352 specific behavior for the splitting of {@link StepExecution}s.
 *
 * @author Michael Minella
 * @since 3.0
 */
public class JsrStepExecutionSplitter extends SimpleStepExecutionSplitter {

	private String stepName;
	private JobRepository jobRepository;
	private boolean restoreState;

	public JsrStepExecutionSplitter(JobRepository jobRepository, boolean allowStartIfComplete, String stepName, boolean restoreState) {
		super(jobRepository, allowStartIfComplete, stepName, null);
		this.stepName = stepName;
		this.jobRepository = jobRepository;
		this.restoreState = restoreState;
	}

	@Override
	public String getStepName() {
		return this.stepName;
	}

	/**
	 * Returns the same number of {@link StepExecution}s as the gridSize specifies.  Each
	 * of the child StepExecutions will <em>not</em> be available via the {@link JsrJobOperator} per
	 * JSR-352.
	 *
	 * @see <a href="https://java.net/projects/jbatch/lists/public/archive/2013-10/message/10">https://java.net/projects/jbatch/lists/public/archive/2013-10/message/10</a>
	 */
	@Override
	public Set<StepExecution> split(StepExecution stepExecution, int gridSize)
			throws JobExecutionException {
		Set<StepExecution> executions = new TreeSet<>(new Comparator<StepExecution>() {

			@Override
			public int compare(StepExecution arg0, StepExecution arg1) {
				String r1 = "";
				String r2 = "";
				if (arg0 != null) {
					r1 = arg0.getStepName();
				}
				if (arg1 != null) {
					r2 = arg1.getStepName();
				}

				return r1.compareTo(r2);
			}
		});
		JobExecution jobExecution = stepExecution.getJobExecution();

		for(int i = 0; i < gridSize; i++) {
			String stepName = this.stepName + ":partition" + i;
			JobExecution curJobExecution = new JobExecution(jobExecution);
			StepExecution curStepExecution = new StepExecution(stepName, curJobExecution);

			if(!restoreState || isStartable(curStepExecution, new ExecutionContext())) {
				executions.add(curStepExecution);
			}
		}

		jobRepository.addAll(executions);

		return executions;
	}
}
