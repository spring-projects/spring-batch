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

package org.springframework.batch.core.repository.dao;

import java.util.Map;

import org.apache.commons.lang.SerializationUtils;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;

public class MapExecutionContextDao implements ExecutionContextDao {

	private static Map<Long, ExecutionContext> contextsByStepExecutionId = TransactionAwareProxyFactory.createTransactionalMap();

	private static Map<Long, ExecutionContext> contextsByJobExecutionId = TransactionAwareProxyFactory.createTransactionalMap();
	
	public static void clear() {
		contextsByJobExecutionId.clear();
		contextsByStepExecutionId.clear();
	}
	
	private static ExecutionContext copy(ExecutionContext original) {
		return (ExecutionContext) SerializationUtils.deserialize(SerializationUtils.serialize(original));
	}


	public ExecutionContext getExecutionContext(StepExecution stepExecution) {
		return copy(contextsByStepExecutionId.get(stepExecution.getId()));
	}

	public void persistExecutionContext(StepExecution stepExecution) {
		contextsByStepExecutionId.put(stepExecution.getId(), copy(stepExecution.getExecutionContext()));
	}
	
	public ExecutionContext getExecutionContext(JobExecution jobExecution) {
		return copy(contextsByJobExecutionId.get(jobExecution.getId()));
	}

	public void persistExecutionContext(JobExecution jobExecution) {
		contextsByJobExecutionId.put(jobExecution.getId(), copy(jobExecution.getExecutionContext()));

	}

}
