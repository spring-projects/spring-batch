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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.persistence.JobParameter;
import org.springframework.batch.core.repository.persistence.converter.JobExecutionConverter;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.util.CollectionUtils;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * @author Mahmoud Ben Hassine
 * @author Yanming Zhou
 * @author Myeongha Shin
 * @since 5.2.0
 */
public class MongoJobExecutionDao implements JobExecutionDao {

	private static final String JOB_EXECUTIONS_COLLECTION_NAME = "JOB_EXECUTION";

	private static final String JOB_EXECUTIONS_SEQUENCE_NAME = "JOB_EXECUTION_SEQ";

	private final MongoOperations mongoOperations;

	private final String jobExecutionsCollectionName;

	private final JobExecutionConverter jobExecutionConverter = new JobExecutionConverter();

	private DataFieldMaxValueIncrementer jobExecutionIncrementer;

	private MongoJobInstanceDao jobInstanceDao;

	public MongoJobExecutionDao(MongoOperations mongoOperations, String collectionPrefix) {
		this.mongoOperations = mongoOperations;
		this.jobExecutionsCollectionName = collectionPrefix + JOB_EXECUTIONS_COLLECTION_NAME;
		this.jobExecutionIncrementer = new MongoSequenceIncrementer(mongoOperations,
				collectionPrefix + JOB_EXECUTIONS_SEQUENCE_NAME);
	}

	public void setJobExecutionIncrementer(DataFieldMaxValueIncrementer jobExecutionIncrementer) {
		this.jobExecutionIncrementer = jobExecutionIncrementer;
	}

	public void setJobInstanceDao(MongoJobInstanceDao jobInstanceDao) {
		this.jobInstanceDao = jobInstanceDao;
	}

	public JobExecution createJobExecution(JobInstance jobInstance, JobParameters jobParameters) {
		long id = jobExecutionIncrementer.nextLongValue();
		JobExecution jobExecution = new JobExecution(id, jobInstance, jobParameters);

		org.springframework.batch.core.repository.persistence.JobExecution jobExecutionToSave = this.jobExecutionConverter
			.fromJobExecution(jobExecution);
		this.mongoOperations.insert(jobExecutionToSave, jobExecutionsCollectionName);

		return jobExecution;
	}

	@Override
	public void updateJobExecution(JobExecution jobExecution) {
		Query query = query(where("jobExecutionId").is(jobExecution.getId()));
		org.springframework.batch.core.repository.persistence.JobExecution jobExecutionToUpdate = this.jobExecutionConverter
			.fromJobExecution(jobExecution);
		this.mongoOperations.findAndReplace(query, jobExecutionToUpdate, jobExecutionsCollectionName);
	}

	@Override
	public List<JobExecution> findJobExecutions(JobInstance jobInstance) {
		Query query = query(where("jobInstanceId").is(jobInstance.getId()))
			.with(Sort.by(Sort.Direction.DESC, "jobExecutionId"));
		List<org.springframework.batch.core.repository.persistence.JobExecution> jobExecutions = this.mongoOperations
			.find(query, org.springframework.batch.core.repository.persistence.JobExecution.class,
					jobExecutionsCollectionName);
		return jobExecutions.stream().map(jobExecution -> convert(jobExecution, jobInstance)).toList();
	}

	@Override
	public JobExecution getLastJobExecution(JobInstance jobInstance) {
		Query query = query(where("jobInstanceId").is(jobInstance.getId()));
		Sort.Order sortOrder = Sort.Order.desc("jobExecutionId");
		org.springframework.batch.core.repository.persistence.JobExecution jobExecution = this.mongoOperations.findOne(
				query.with(Sort.by(sortOrder)),
				org.springframework.batch.core.repository.persistence.JobExecution.class, jobExecutionsCollectionName);
		return jobExecution != null ? convert(jobExecution, jobInstance) : null;
	}

	@Override
	public Set<JobExecution> findRunningJobExecutions(String jobName) {
		List<JobInstance> jobInstances = this.jobInstanceDao.findJobInstancesByName(jobName);
		Set<JobExecution> runningJobExecutions = new HashSet<>();
		for (JobInstance jobInstance : jobInstances) {
			Query query = query(
					where("jobInstanceId").is(jobInstance.getId()).and("status").in("STARTING", "STARTED", "STOPPING"));
			this.mongoOperations
				.find(query, org.springframework.batch.core.repository.persistence.JobExecution.class,
						jobExecutionsCollectionName)
				.stream()
				.map(jobExecution -> convert(jobExecution, jobInstance))
				.forEach(runningJobExecutions::add);
		}
		return runningJobExecutions;
	}

	@Override
	public JobExecution getJobExecution(long executionId) {
		Query jobExecutionQuery = query(where("jobExecutionId").is(executionId));
		org.springframework.batch.core.repository.persistence.JobExecution jobExecution = this.mongoOperations.findOne(
				jobExecutionQuery, org.springframework.batch.core.repository.persistence.JobExecution.class,
				jobExecutionsCollectionName);
		if (jobExecution == null) {
			return null;
		}
		org.springframework.batch.core.job.JobInstance jobInstance = this.jobInstanceDao
			.getJobInstance(jobExecution.getJobInstanceId());
		return convert(jobExecution, jobInstance);
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

	@Override
	public void deleteJobExecution(JobExecution jobExecution) {
		this.mongoOperations.remove(query(where("jobExecutionId").is(jobExecution.getId())),
				this.jobExecutionsCollectionName);

	}

	@Override
	public void deleteJobExecutionParameters(JobExecution jobExecution) {
		Query query = new Query(where("jobExecutionId").is(jobExecution.getId()));
		Update jobParametersRemovalUpdate = new Update().set("jobParameters", Collections.emptyList());
		this.mongoOperations.updateFirst(query, jobParametersRemovalUpdate, this.jobExecutionsCollectionName);
	}

	private JobExecution convert(org.springframework.batch.core.repository.persistence.JobExecution jobExecution,
			org.springframework.batch.core.job.JobInstance jobInstance) {
		Set<JobParameter<?>> parameters = jobExecution.getJobParameters();
		if (!CollectionUtils.isEmpty(parameters)) {
			// MongoDB restore temporal value as Date
			Set<JobParameter<?>> converted = new HashSet<>();
			for (JobParameter<?> parameter : parameters) {
				if (LocalDate.class.getName().equals(parameter.type()) && parameter.value() instanceof Date date) {
					converted.add(new JobParameter<>(parameter.name(),
							date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), parameter.type(),
							parameter.identifying()));
				}
				else if (LocalTime.class.getName().equals(parameter.type()) && parameter.value() instanceof Date date) {
					converted.add(new JobParameter<>(parameter.name(),
							date.toInstant().atZone(ZoneId.systemDefault()).toLocalTime(), parameter.type(),
							parameter.identifying()));
				}
				else if (LocalDateTime.class.getName().equals(parameter.type())
						&& parameter.value() instanceof Date date) {
					converted.add(new JobParameter<>(parameter.name(),
							date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(), parameter.type(),
							parameter.identifying()));
				}
				else {
					converted.add(parameter);
				}
			}
			jobExecution.setJobParameters(converted);
		}
		return this.jobExecutionConverter.toJobExecution(jobExecution, jobInstance);
	}

}
