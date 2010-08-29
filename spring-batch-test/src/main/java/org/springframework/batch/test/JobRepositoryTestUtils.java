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
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcOperations;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.util.Assert;

/**
 * Convenience class for creating and removing {@link JobExecution} instances
 * from a database. Typical usage in test case would be to create instances
 * before a transaction, save the result, and then use it to remove them after
 * the transaction.
 * 
 * @author Dave Syer
 */
public class JobRepositoryTestUtils extends AbstractJdbcBatchMetadataDao implements InitializingBean {

	private JobRepository jobRepository;

	private JobParametersIncrementer jobParametersIncrementer = new JobParametersIncrementer() {

		Long count = 0L;

		public JobParameters getNext(JobParameters parameters) {
			return new JobParameters(Collections.singletonMap("count", new JobParameter(count++)));
		}

	};

	private SimpleJdbcOperations jdbcTemplate;

	/**
	 * @see InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(jobRepository, "JobRepository must be set");
		Assert.notNull(jdbcTemplate, "DataSource must be set");
	}

	/**
	 * Default constructor.
	 */
	public JobRepositoryTestUtils() {
	}

	/**
	 * Create a {@link JobRepositoryTestUtils} with all its mandatory
	 * properties.
	 * 
	 * @param jobRepository a {@link JobRepository} backed by a database
	 * @param dataSource a {@link DataSource}
	 */
	public JobRepositoryTestUtils(JobRepository jobRepository, DataSource dataSource) {
		super();
		this.jobRepository = jobRepository;
		setDataSource(dataSource);
	}

	public final void setDataSource(DataSource dataSource) {
		jdbcTemplate = new SimpleJdbcTemplate(dataSource);
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
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * Use the {@link JobRepository} to create some {@link JobExecution}
	 * instances each with the given job name and each having step executions
	 * with the given step names.
	 * 
	 * @param jobName the name of the job
	 * @param stepNames the names of the step executions
	 * @param count the required number of instances of {@link JobExecution} to
	 * create
	 * @return a collection of {@link JobExecution}
	 * @throws Exception if there is a problem in the {@link JobRepository}
	 */
	public List<JobExecution> createJobExecutions(String jobName, String[] stepNames, int count)
			throws JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {
		List<JobExecution> list = new ArrayList<JobExecution>();
		JobParameters jobParameters = new JobParameters();
		for (int i = 0; i < count; i++) {
			JobExecution jobExecution = jobRepository.createJobExecution(jobName, jobParametersIncrementer
					.getNext(jobParameters));
			list.add(jobExecution);
			for (String stepName : stepNames) {
				jobRepository.add(jobExecution.createStepExecution(stepName));
			}
		}
		return list;
	}

	/**
	 * Use the {@link JobRepository} to create some {@link JobExecution}
	 * instances each with a single step execution.
	 * 
	 * @param count the required number of instances of {@link JobExecution} to
	 * create
	 * @return a collection of {@link JobExecution}
	 * @throws Exception if there is a problem in the {@link JobRepository}
	 */
	public List<JobExecution> createJobExecutions(int count) throws JobExecutionAlreadyRunningException,
			JobRestartException, JobInstanceAlreadyCompleteException {
		return createJobExecutions("job", new String[] { "step" }, count);
	}

	/**
	 * Remove the {@link JobExecution} instances, and all associated
	 * {@link JobInstance} and {@link StepExecution} instances from the standard
	 * RDBMS locations used by Spring Batch.
	 * 
	 * @param list a list of {@link JobExecution}
	 * @throws DataAccessException if there is a problem
	 */
	public void removeJobExecutions(Collection<JobExecution> list) throws DataAccessException {
		for (JobExecution jobExecution : list) {
			List<Long> stepExecutionIds = jdbcTemplate.query(
					getQuery("select STEP_EXECUTION_ID from %PREFIX%STEP_EXECUTION where JOB_EXECUTION_ID=?"),
					new ParameterizedRowMapper<Long>() {
						public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
							return rs.getLong(1);
						}
					}, jobExecution.getId());
			for (Long stepExecutionId : stepExecutionIds) {
				jdbcTemplate.update(getQuery("delete from %PREFIX%STEP_EXECUTION_CONTEXT where STEP_EXECUTION_ID=?"),
						stepExecutionId);
				jdbcTemplate.update(getQuery("delete from %PREFIX%STEP_EXECUTION where STEP_EXECUTION_ID=?"),
						stepExecutionId);
			}
			jdbcTemplate.update(getQuery("delete from %PREFIX%JOB_EXECUTION_CONTEXT where JOB_EXECUTION_ID=?"),
					jobExecution.getId());
			jdbcTemplate.update(getQuery("delete from %PREFIX%JOB_EXECUTION where JOB_EXECUTION_ID=?"), jobExecution
					.getId());
		}
		for (JobExecution jobExecution : list) {		
			jdbcTemplate.update(getQuery("delete from %PREFIX%JOB_PARAMS where JOB_INSTANCE_ID=?"), jobExecution
					.getJobId());
			jdbcTemplate.update(getQuery("delete from %PREFIX%JOB_INSTANCE where JOB_INSTANCE_ID=?"), jobExecution
					.getJobId());
		}
	}

	/**
	 * Remove all the {@link JobExecution} instances, and all associated
	 * {@link JobInstance} and {@link StepExecution} instances from the standard
	 * RDBMS locations used by Spring Batch.
	 * 
	 * @param list a list of {@link JobExecution}
	 * @throws DataAccessException if there is a problem
	 */
	public void removeJobExecutions() throws DataAccessException {

		jdbcTemplate.update(getQuery("delete from %PREFIX%STEP_EXECUTION_CONTEXT"));
		jdbcTemplate.update(getQuery("delete from %PREFIX%STEP_EXECUTION"));
		jdbcTemplate.update(getQuery("delete from %PREFIX%JOB_EXECUTION_CONTEXT"));
		jdbcTemplate.update(getQuery("delete from %PREFIX%JOB_EXECUTION"));
		jdbcTemplate.update(getQuery("delete from %PREFIX%JOB_PARAMS"));
		jdbcTemplate.update(getQuery("delete from %PREFIX%JOB_INSTANCE"));

	}

}
