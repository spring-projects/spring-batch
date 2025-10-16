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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.batch.core.repository.persistence.converter.JobExecutionConverter;
import org.springframework.batch.core.repository.persistence.converter.StepExecutionConverter;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * @author Mahmoud Ben Hassine
 * @author Yanming Zhou
 * @author Myeongha Shin
 * @since 5.2.0
 */
public class MongoStepExecutionDao implements StepExecutionDao {

	private static final String STEP_EXECUTIONS_COLLECTION_NAME = "STEP_EXECUTION";

	private static final String STEP_EXECUTIONS_SEQUENCE_NAME = "STEP_EXECUTION_SEQ";

	private static final String JOB_EXECUTIONS_COLLECTION_NAME = "JOB_EXECUTION";

	private final String stepExecutionCollectionName;

	private final String jobExecutionCollectionName;

	private final StepExecutionConverter stepExecutionConverter = new StepExecutionConverter();

	private final JobExecutionConverter jobExecutionConverter = new JobExecutionConverter();

	private final MongoOperations mongoOperations;

	private DataFieldMaxValueIncrementer stepExecutionIncrementer;

	MongoJobExecutionDao jobExecutionDao;

	public MongoStepExecutionDao(MongoOperations mongoOperations, String collectionPrefix) {
		this.mongoOperations = mongoOperations;
		this.stepExecutionCollectionName = collectionPrefix + STEP_EXECUTIONS_COLLECTION_NAME;
		this.jobExecutionCollectionName = collectionPrefix + JOB_EXECUTIONS_COLLECTION_NAME;
		this.stepExecutionIncrementer = new MongoSequenceIncrementer(mongoOperations,
				collectionPrefix + STEP_EXECUTIONS_SEQUENCE_NAME);
	}

	public void setStepExecutionIncrementer(DataFieldMaxValueIncrementer stepExecutionIncrementer) {
		this.stepExecutionIncrementer = stepExecutionIncrementer;
	}

	public void setJobExecutionDao(MongoJobExecutionDao jobExecutionDao) {
		this.jobExecutionDao = jobExecutionDao;
	}

	public StepExecution createStepExecution(String stepName, JobExecution jobExecution) {
		long id = stepExecutionIncrementer.nextLongValue();

		StepExecution stepExecution = new StepExecution(id, stepName, jobExecution);
		org.springframework.batch.core.repository.persistence.StepExecution stepExecutionToSave = this.stepExecutionConverter
			.fromStepExecution(stepExecution);
		this.mongoOperations.insert(stepExecutionToSave, stepExecutionCollectionName);

		return stepExecution;
	}

	@Override
	public void updateStepExecution(StepExecution stepExecution) {
		Query query = query(where("stepExecutionId").is(stepExecution.getId()));
		org.springframework.batch.core.repository.persistence.StepExecution stepExecutionToUpdate = this.stepExecutionConverter
			.fromStepExecution(stepExecution);
		this.mongoOperations.findAndReplace(query, stepExecutionToUpdate, this.stepExecutionCollectionName);
	}

	@Nullable
	@Override
	public StepExecution getStepExecution(long stepExecutionId) {
		Query query = query(where("stepExecutionId").is(stepExecutionId));
		org.springframework.batch.core.repository.persistence.StepExecution stepExecution = this.mongoOperations
			.findOne(query, org.springframework.batch.core.repository.persistence.StepExecution.class,
					this.stepExecutionCollectionName);
		return stepExecution != null ? this.stepExecutionConverter.toStepExecution(stepExecution,
				jobExecutionDao.getJobExecution(stepExecution.getJobExecutionId())) : null;
	}

	@Deprecated(since = "6.0", forRemoval = true)
	@Override
	public StepExecution getStepExecution(JobExecution jobExecution, long stepExecutionId) {
		Query query = query(where("stepExecutionId").is(stepExecutionId));
		org.springframework.batch.core.repository.persistence.StepExecution stepExecution = this.mongoOperations
			.findOne(query, org.springframework.batch.core.repository.persistence.StepExecution.class,
					stepExecutionCollectionName);
		return stepExecution != null ? this.stepExecutionConverter.toStepExecution(stepExecution, jobExecution) : null;
	}

	@Nullable
	@Override
	public StepExecution getLastStepExecution(JobInstance jobInstance, String stepName) {
		// TODO optimize the query
		// get all step executions
		Query query = query(where("jobInstanceId").is(jobInstance.getId()));
		List<org.springframework.batch.core.repository.persistence.JobExecution> jobExecutions = this.mongoOperations
			.find(query, org.springframework.batch.core.repository.persistence.JobExecution.class,
					jobExecutionCollectionName);
		List<org.springframework.batch.core.repository.persistence.StepExecution> stepExecutions = this.mongoOperations
			.find(query(where("jobExecutionId").in(jobExecutions.stream()
				.map(org.springframework.batch.core.repository.persistence.JobExecution::getJobExecutionId)
				.toList())), org.springframework.batch.core.repository.persistence.StepExecution.class,
					stepExecutionCollectionName);
		// sort step executions by creation date then id (see contract) and return the
		// last one
		Optional<org.springframework.batch.core.repository.persistence.StepExecution> lastStepExecution = stepExecutions
			.stream()
			.filter(stepExecution -> stepExecution.getName().equals(stepName))
			.max(Comparator
				.comparing(org.springframework.batch.core.repository.persistence.StepExecution::getCreateTime)
				.thenComparing(
						org.springframework.batch.core.repository.persistence.StepExecution::getStepExecutionId));
		if (lastStepExecution.isPresent()) {
			org.springframework.batch.core.repository.persistence.StepExecution stepExecution = lastStepExecution.get();
			JobExecution jobExecution = this.jobExecutionConverter.toJobExecution(jobExecutions.stream()
				.filter(execution -> execution.getJobExecutionId() == stepExecution.getJobExecutionId())
				.findFirst()
				.get(), jobInstance);
			return this.stepExecutionConverter.toStepExecution(stepExecution, jobExecution);
		}
		else {
			return null;
		}
	}

	/**
	 * Retrieve all {@link StepExecution}s for a given {@link JobExecution}.
	 * @param jobExecution the parent {@link JobExecution}
	 * @return a collection of {@link StepExecution}s
	 * @since 6.0
	 */
	@Override
	public List<StepExecution> getStepExecutions(JobExecution jobExecution) {
		Query query = query(where("jobExecutionId").is(jobExecution.getId()));
		return this.mongoOperations
			.find(query, org.springframework.batch.core.repository.persistence.StepExecution.class,
					stepExecutionCollectionName)
			.stream()
			.map(stepExecution -> this.stepExecutionConverter.toStepExecution(stepExecution, jobExecution))
			.toList();
	}

	@Override
	public long countStepExecutions(JobInstance jobInstance, String stepName) {
		Query query = query(where("jobInstanceId").is(jobInstance.getId()));
		List<org.springframework.batch.core.repository.persistence.JobExecution> jobExecutions = this.mongoOperations
			.find(query, org.springframework.batch.core.repository.persistence.JobExecution.class,
					jobExecutionCollectionName);
		return this.mongoOperations.count(
				query(where("jobExecutionId").in(jobExecutions.stream()
					.map(org.springframework.batch.core.repository.persistence.JobExecution::getJobExecutionId)
					.toList())),
				org.springframework.batch.core.repository.persistence.StepExecution.class,
				stepExecutionCollectionName);
	}

	@Override
	public void deleteStepExecution(StepExecution stepExecution) {
		this.mongoOperations.remove(query(where("stepExecutionId").is(stepExecution.getId())),
				stepExecutionCollectionName);
	}

}
