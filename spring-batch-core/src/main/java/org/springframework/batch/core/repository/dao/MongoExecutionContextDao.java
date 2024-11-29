/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.batch.core.repository.dao;

import java.util.Collection;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * @author Mahmoud Ben Hassine
 * @since 5.2.0
 */
public class MongoExecutionContextDao implements ExecutionContextDao {

	private static final String STEP_EXECUTIONS_COLLECTION_NAME = "BATCH_STEP_EXECUTION";

	private static final String JOB_EXECUTIONS_COLLECTION_NAME = "BATCH_JOB_EXECUTION";

	private final MongoOperations mongoOperations;

	public MongoExecutionContextDao(MongoOperations mongoOperations) {
		this.mongoOperations = mongoOperations;
	}

	@Override
	public ExecutionContext getExecutionContext(JobExecution jobExecution) {
		Query query = query(where("jobExecutionId").is(jobExecution.getId()));
		org.springframework.batch.core.repository.persistence.JobExecution execution = this.mongoOperations.findOne(
				query, org.springframework.batch.core.repository.persistence.JobExecution.class,
				JOB_EXECUTIONS_COLLECTION_NAME);
		if (execution == null) {
			return new ExecutionContext();
		}
		return new ExecutionContext(execution.getExecutionContext().map());
	}

	@Override
	public ExecutionContext getExecutionContext(StepExecution stepExecution) {
		Query query = query(where("stepExecutionId").is(stepExecution.getId()));
		org.springframework.batch.core.repository.persistence.StepExecution execution = this.mongoOperations.findOne(
				query, org.springframework.batch.core.repository.persistence.StepExecution.class,
				STEP_EXECUTIONS_COLLECTION_NAME);
		if (execution == null) {
			return new ExecutionContext();
		}
		return new ExecutionContext(execution.getExecutionContext().map());
	}

	@Override
	public void saveExecutionContext(JobExecution jobExecution) {
		ExecutionContext executionContext = jobExecution.getExecutionContext();
		Query query = query(where("_id").is(jobExecution.getId()));

		Update update = Update.update("executionContext",
				new org.springframework.batch.core.repository.persistence.ExecutionContext(executionContext.toMap(),
						executionContext.isDirty()));
		this.mongoOperations.updateFirst(query, update,
				org.springframework.batch.core.repository.persistence.JobExecution.class,
				JOB_EXECUTIONS_COLLECTION_NAME);
	}

	@Override
	public void saveExecutionContext(StepExecution stepExecution) {
		ExecutionContext executionContext = stepExecution.getExecutionContext();
		Query query = query(where("_id").is(stepExecution.getId()));

		Update update = Update.update("executionContext",
				new org.springframework.batch.core.repository.persistence.ExecutionContext(executionContext.toMap(),
						executionContext.isDirty()));
		this.mongoOperations.updateFirst(query, update,
				org.springframework.batch.core.repository.persistence.StepExecution.class,
				STEP_EXECUTIONS_COLLECTION_NAME);

	}

	@Override
	public void saveExecutionContexts(Collection<StepExecution> stepExecutions) {
		for (StepExecution stepExecution : stepExecutions) {
			saveExecutionContext(stepExecution);
		}
	}

	@Override
	public void updateExecutionContext(JobExecution jobExecution) {
		saveExecutionContext(jobExecution);
	}

	@Override
	public void updateExecutionContext(StepExecution stepExecution) {
		saveExecutionContext(stepExecution);
	}

}
