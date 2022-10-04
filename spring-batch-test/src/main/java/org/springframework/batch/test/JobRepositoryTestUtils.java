/*
 * Copyright 2006-2022 the original author or authors.
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
package org.springframework.batch.test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.repository.dao.AbstractJdbcBatchMetadataDao;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Convenience class for creating and removing {@link JobExecution} instances from a
 * database. Typical usage in test case would be to create instances before a transaction,
 * save the result, and then use it to remove them after the transaction.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 */
public class JobRepositoryTestUtils {

	private JobRepository jobRepository;

	private JobParametersIncrementer jobParametersIncrementer = new JobParametersIncrementer() {

		Long count = 0L;

		@Override
		public JobParameters getNext(@Nullable JobParameters parameters) {
			return new JobParameters(Collections.singletonMap("count", new JobParameter(count++, Long.class)));
		}

	};

	/**
	 * Default constructor.
	 */
	public JobRepositoryTestUtils() {
	}

	/**
	 * Create a {@link JobRepositoryTestUtils} with all its mandatory properties.
	 * @param jobRepository a {@link JobRepository}.
	 */
	public JobRepositoryTestUtils(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * @param jobParametersIncrementer the jobParametersIncrementer to set
	 */
	public void setJobParametersIncrementer(JobParametersIncrementer jobParametersIncrementer) {
		this.jobParametersIncrementer = jobParametersIncrementer;
	}

	/**
	 * @param jobRepository the jobRepository to set
	 */
	@Autowired
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * Use the {@link JobRepository} to create some {@link JobExecution} instances each
	 * with the given job name and each having step executions with the given step names.
	 * @param jobName the name of the job
	 * @param stepNames the names of the step executions
	 * @param count the required number of instances of {@link JobExecution} to create
	 * @return a collection of {@link JobExecution}
	 * @throws JobExecutionAlreadyRunningException thrown if Job is already running.
	 * @throws JobRestartException thrown if Job is not restartable.
	 * @throws JobInstanceAlreadyCompleteException thrown if Job Instance is already
	 * complete.
	 */
	public List<JobExecution> createJobExecutions(String jobName, String[] stepNames, int count)
			throws JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {
		List<JobExecution> list = new ArrayList<>();
		JobParameters jobParameters = new JobParameters();
		for (int i = 0; i < count; i++) {
			JobExecution jobExecution = jobRepository.createJobExecution(jobName,
					jobParametersIncrementer.getNext(jobParameters));
			list.add(jobExecution);
			for (String stepName : stepNames) {
				jobRepository.add(jobExecution.createStepExecution(stepName));
			}
		}
		return list;
	}

	/**
	 * Use the {@link JobRepository} to create some {@link JobExecution} instances each
	 * with a single step execution.
	 * @param count the required number of instances of {@link JobExecution} to create
	 * @return a collection of {@link JobExecution}
	 * @throws JobExecutionAlreadyRunningException thrown if Job is already running.
	 * @throws JobRestartException thrown if Job is not restartable.
	 * @throws JobInstanceAlreadyCompleteException thrown if Job Instance is already
	 * complete.
	 */
	public List<JobExecution> createJobExecutions(int count)
			throws JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {
		return createJobExecutions("job", new String[] { "step" }, count);
	}

	/**
	 * Remove the {@link JobExecution} instances, and all associated {@link JobInstance}
	 * and {@link StepExecution} instances from the standard locations used by Spring
	 * Batch.
	 * @param jobExecutions a collection of {@link JobExecution}
	 */
	public void removeJobExecutions(Collection<JobExecution> jobExecutions) {
		for (JobExecution jobExecution : jobExecutions) {
			removeJobExecution(jobExecution);
		}
		for (JobExecution jobExecution : jobExecutions) {
			this.jobRepository.deleteJobInstance(jobExecution.getJobInstance());
		}
	}

	/**
	 * Remove the {@link JobExecution} and its associated {@link StepExecution} instances
	 * Ôfrom the standard locations used by Spring Batch.
	 * @param jobExecution the {@link JobExecution} to delete
	 */
	public void removeJobExecution(JobExecution jobExecution) {
		for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
			this.jobRepository.deleteStepExecution(stepExecution);
		}
		this.jobRepository.deleteJobExecution(jobExecution);
	}

	/**
	 * Remove all the {@link JobExecution} instances, and all associated
	 * {@link JobInstance} and {@link StepExecution} instances from the standard locations
	 * used by Spring Batch.
	 */
	public void removeJobExecutions() {
		List<String> jobNames = this.jobRepository.getJobNames();
		for (String jobName : jobNames) {
			int start = 0;
			int count = 100;
			List<JobInstance> jobInstances = this.jobRepository.findJobInstancesByName(jobName, start, count);
			while (!jobInstances.isEmpty()) {
				for (JobInstance jobInstance : jobInstances) {
					List<JobExecution> jobExecutions = this.jobRepository.findJobExecutions(jobInstance);
					if (jobExecutions != null && !jobExecutions.isEmpty()) {
						removeJobExecutions(jobExecutions);
					}
				}
				start += count;
				jobInstances = this.jobRepository.findJobInstancesByName(jobName, start, count);
			}
		}
	}

}
