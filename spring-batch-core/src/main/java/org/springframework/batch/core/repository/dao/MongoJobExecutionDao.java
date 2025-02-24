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
package org.springframework.batch.core.repository.dao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.repository.persistence.converter.JobExecutionConverter;
import org.springframework.batch.core.repository.persistence.converter.JobInstanceConverter;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * @author Mahmoud Ben Hassine
 * @since 5.2.0
 */
public class MongoJobExecutionDao implements JobExecutionDao {

	private static final String JOB_EXECUTIONS_COLLECTION_NAME = "BATCH_JOB_EXECUTION";

	private static final String JOB_EXECUTIONS_SEQUENCE_NAME = "BATCH_JOB_EXECUTION_SEQ";

	private static final String JOB_INSTANCES_COLLECTION_NAME = "BATCH_JOB_INSTANCE";

	private final MongoOperations mongoOperations;

	private final JobExecutionConverter jobExecutionConverter = new JobExecutionConverter();

	private final JobInstanceConverter jobInstanceConverter = new JobInstanceConverter();

	private DataFieldMaxValueIncrementer jobExecutionIncrementer;

	public MongoJobExecutionDao(MongoOperations mongoOperations) {
		this.mongoOperations = mongoOperations;
		this.jobExecutionIncrementer = new MongoSequenceIncrementer(mongoOperations, JOB_EXECUTIONS_SEQUENCE_NAME);
	}

	public void setJobExecutionIncrementer(DataFieldMaxValueIncrementer jobExecutionIncrementer) {
		this.jobExecutionIncrementer = jobExecutionIncrementer;
	}

	@Override
	public void saveJobExecution(JobExecution jobExecution) {
		org.springframework.batch.core.repository.persistence.JobExecution jobExecutionToSave = this.jobExecutionConverter
			.fromJobExecution(jobExecution);
		long jobExecutionId = this.jobExecutionIncrementer.nextLongValue();
		jobExecutionToSave.setJobExecutionId(jobExecutionId);
		this.mongoOperations.insert(jobExecutionToSave, JOB_EXECUTIONS_COLLECTION_NAME);
		jobExecution.setId(jobExecutionId);
	}

	@Override
	public void updateJobExecution(JobExecution jobExecution) {
		Query query = query(where("jobExecutionId").is(jobExecution.getId()));
		org.springframework.batch.core.repository.persistence.JobExecution jobExecutionToUpdate = this.jobExecutionConverter
			.fromJobExecution(jobExecution);
		this.mongoOperations.findAndReplace(query, jobExecutionToUpdate, JOB_EXECUTIONS_COLLECTION_NAME);
	}

	@Override
	public List<JobExecution> findJobExecutions(JobInstance jobInstance) {
		Query query = query(where("jobInstanceId").is(jobInstance.getId()));
		List<org.springframework.batch.core.repository.persistence.JobExecution> jobExecutions = this.mongoOperations
			.find(query, org.springframework.batch.core.repository.persistence.JobExecution.class,
					JOB_EXECUTIONS_COLLECTION_NAME);
		return jobExecutions.stream()
			.map(jobExecution -> this.jobExecutionConverter.toJobExecution(jobExecution, jobInstance))
			.toList();
	}

	@Override
	public JobExecution getLastJobExecution(JobInstance jobInstance) {
		Query query = query(where("jobInstanceId").is(jobInstance.getId()));
		Sort.Order sortOrder = Sort.Order.desc("jobExecutionId");
		org.springframework.batch.core.repository.persistence.JobExecution jobExecution = this.mongoOperations.findOne(
				query.with(Sort.by(sortOrder)),
				org.springframework.batch.core.repository.persistence.JobExecution.class,
				JOB_EXECUTIONS_COLLECTION_NAME);
		return jobExecution != null ? this.jobExecutionConverter.toJobExecution(jobExecution, jobInstance) : null;
	}

	@Override
	public Set<JobExecution> findRunningJobExecutions(String jobName) {
		Query query = query(where("jobName").is(jobName));
		List<JobInstance> jobInstances = this.mongoOperations
			.find(query, org.springframework.batch.core.repository.persistence.JobInstance.class,
					JOB_INSTANCES_COLLECTION_NAME)
			.stream()
			.map(this.jobInstanceConverter::toJobInstance)
			.toList();
		Set<JobExecution> runningJobExecutions = new HashSet<>();
		for (JobInstance jobInstance : jobInstances) {
			query = query(
					where("jobInstanceId").is(jobInstance.getId()).and("status").in("STARTING", "STARTED", "STOPPING"));
			this.mongoOperations
				.find(query, org.springframework.batch.core.repository.persistence.JobExecution.class,
						JOB_EXECUTIONS_COLLECTION_NAME)
				.stream()
				.map(jobExecution -> this.jobExecutionConverter.toJobExecution(jobExecution, jobInstance))
				.forEach(runningJobExecutions::add);
		}
		return runningJobExecutions;
	}

	@Override
	public JobExecution getJobExecution(Long executionId) {
		Query jobExecutionQuery = query(where("jobExecutionId").is(executionId));
		org.springframework.batch.core.repository.persistence.JobExecution jobExecution = this.mongoOperations.findOne(
				jobExecutionQuery, org.springframework.batch.core.repository.persistence.JobExecution.class,
				JOB_EXECUTIONS_COLLECTION_NAME);
		if (jobExecution == null) {
			return null;
		}
		Query jobInstanceQuery = query(where("jobInstanceId").is(jobExecution.getJobInstanceId()));
		org.springframework.batch.core.repository.persistence.JobInstance jobInstance = this.mongoOperations.findOne(
				jobInstanceQuery, org.springframework.batch.core.repository.persistence.JobInstance.class,
				JOB_INSTANCES_COLLECTION_NAME);
		return this.jobExecutionConverter.toJobExecution(jobExecution,
				this.jobInstanceConverter.toJobInstance(jobInstance));
	}

	@Override
	public void synchronizeStatus(JobExecution jobExecution) {
		JobExecution currentJobExecution = getJobExecution(jobExecution.getId());
		if (currentJobExecution != null && currentJobExecution.getStatus().isGreaterThan(jobExecution.getStatus())) {
			jobExecution.upgradeStatus(currentJobExecution.getStatus());
		}
		// TODO the contract mentions to update the version as well. Double check if this
		// is needed as the version is not used in the tests following the call sites of
		// synchronizeStatus
	}

}
