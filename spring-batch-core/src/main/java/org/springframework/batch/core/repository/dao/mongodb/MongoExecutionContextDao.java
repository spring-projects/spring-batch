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
package org.springframework.batch.core.repository.dao.mongodb;

import java.util.Collection;
import java.util.Collections;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.Assert;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * @author Mahmoud Ben Hassine
 * @author Myeongha Shin
 * @since 5.2.0
 */
public class MongoExecutionContextDao implements ExecutionContextDao {

	private static final String STEP_EXECUTIONS_COLLECTION_NAME = "STEP_EXECUTION";

	private static final String JOB_EXECUTIONS_COLLECTION_NAME = "JOB_EXECUTION";

	private final MongoOperations mongoOperations;

	private final String stepExecutionCollectionName;

	private final String jobExecutionCollectionName;

	public MongoExecutionContextDao(MongoOperations mongoOperations, String collectionPrefix) {
		Assert.notNull(mongoOperations, "mongoOperations must not be null.");
		Assert.notNull(collectionPrefix, "collectionPrefix must not be null.");
		this.mongoOperations = mongoOperations;
		this.stepExecutionCollectionName = collectionPrefix + STEP_EXECUTIONS_COLLECTION_NAME;
		this.jobExecutionCollectionName = collectionPrefix + JOB_EXECUTIONS_COLLECTION_NAME;
	}

	@Override
	public ExecutionContext getExecutionContext(JobExecution jobExecution) {
		Query query = query(where("jobExecutionId").is(jobExecution.getId()));
		org.springframework.batch.core.repository.persistence.JobExecution execution = this.mongoOperations.findOne(
				query, org.springframework.batch.core.repository.persistence.JobExecution.class,
				jobExecutionCollectionName);
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
				stepExecutionCollectionName);
		if (execution == null) {
			return new ExecutionContext();
		}
		return new ExecutionContext(execution.getExecutionContext().map());
	}

	@Override
	public void saveExecutionContext(JobExecution jobExecution) {
		ExecutionContext executionContext = jobExecution.getExecutionContext();
		Query query = query(where("jobExecutionId").is(jobExecution.getId()));

		Update update = Update.update("executionContext",
				new org.springframework.batch.core.repository.persistence.ExecutionContext(executionContext.toMap(),
						executionContext.isDirty()));
		this.mongoOperations.updateFirst(query, update,
				org.springframework.batch.core.repository.persistence.JobExecution.class, jobExecutionCollectionName);
	}

	@Override
	public void saveExecutionContext(StepExecution stepExecution) {
		ExecutionContext executionContext = stepExecution.getExecutionContext();
		Query query = query(where("stepExecutionId").is(stepExecution.getId()));

		Update update = Update.update("executionContext",
				new org.springframework.batch.core.repository.persistence.ExecutionContext(executionContext.toMap(),
						executionContext.isDirty()));
		this.mongoOperations.updateFirst(query, update,
				org.springframework.batch.core.repository.persistence.StepExecution.class, stepExecutionCollectionName);

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

	@Override
	public void deleteExecutionContext(JobExecution jobExecution) {
		Query query = new Query(where("jobExecutionId").is(jobExecution.getId()));
		org.springframework.batch.core.repository.persistence.ExecutionContext executionContext = new org.springframework.batch.core.repository.persistence.ExecutionContext(
				Collections.emptyMap(), false);
		Update executionContextRemovalUpdate = new Update().set("executionContext", executionContext);
		this.mongoOperations.updateFirst(query, executionContextRemovalUpdate, this.jobExecutionCollectionName);
	}

	@Override
	public void deleteExecutionContext(StepExecution stepExecution) {
		Query query = new Query(where("stepExecutionId").is(stepExecution.getId()));
		org.springframework.batch.core.repository.persistence.ExecutionContext executionContext = new org.springframework.batch.core.repository.persistence.ExecutionContext(
				Collections.emptyMap(), false);
		Update executionContextRemovalUpdate = new Update().set("executionContext", executionContext);
		this.mongoOperations.updateFirst(query, executionContextRemovalUpdate, this.stepExecutionCollectionName);
	}

}
