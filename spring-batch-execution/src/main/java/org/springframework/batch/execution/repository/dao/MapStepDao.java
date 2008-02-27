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

package org.springframework.batch.execution.repository.dao;

import java.util.Map;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.Step;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.util.Assert;

public class MapStepDao implements StepExecutionDao {

	private static Map executionsByJobExecutionId;

	private static Map contextsByStepExecutionId;

	private static long currentId = 0;

	static {
		executionsByJobExecutionId = TransactionAwareProxyFactory.createTransactionalMap();
		contextsByStepExecutionId = TransactionAwareProxyFactory.createTransactionalMap();
	}

	public static void clear() {
		executionsByJobExecutionId.clear();
	}

	public ExecutionContext findExecutionContext(StepExecution stepExecution) {
		return (ExecutionContext) contextsByStepExecutionId.get(stepExecution.getId());
	}

	public void saveOrUpdateExecutionContext(StepExecution stepExecution) {
		contextsByStepExecutionId.put(stepExecution.getId(), stepExecution.getExecutionContext());
	}

	public void saveStepExecution(StepExecution stepExecution) {
		Assert.notNull(stepExecution.getJobExecutionId());
		Map executions = (Map) executionsByJobExecutionId.get(stepExecution.getJobExecutionId());
		if (executions == null) {
			executions = TransactionAwareProxyFactory.createTransactionalMap();
			executionsByJobExecutionId.put(stepExecution.getJobExecutionId(), executions);
		}
		stepExecution.setId(new Long(currentId++));
		executions.put(stepExecution.getStepName(), stepExecution);
	}

	public void updateStepExecution(StepExecution stepExecution) {
		Assert.notNull(stepExecution.getJobExecutionId());
		Map executions = (Map) executionsByJobExecutionId.get(stepExecution.getJobExecutionId());
		Assert.notNull(executions, "step executions for given job execution are expected to be already saved");
		Assert.notNull(executions.get(stepExecution.getStepName()), "step execution is expected to be already saved");
		executions.put(stepExecution.getStepName(), stepExecution);
	}

	public StepExecution getStepExecution(JobExecution jobExecution, Step step) {
		Map executions = (Map) executionsByJobExecutionId.get(jobExecution.getId());
		if (executions == null) {
			return null;
		}

		return (StepExecution) executions.get(step.getName());
	}

}
