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

package org.springframework.batch.core.repository.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public abstract class AbstractJobDaoTests {

	protected JobInstanceDao jobInstanceDao;

	protected JobExecutionDao jobExecutionDao;

	protected JobParameters jobParameters = new JobParametersBuilder().addString("job.key", "jobKey")
		.addLong("long", 1L)
		.addDouble("double", 7.7)
		.toJobParameters();

	protected JobInstance jobInstance;

	protected String jobName = "Job1";

	protected JobExecution jobExecution;

	protected LocalDateTime jobExecutionStartTime = LocalDateTime.now();

	protected JdbcTemplate jdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	/*
	 * Because AbstractTransactionalSpringContextTests is used, this method will be called
	 * by Spring to set the JobRepository.
	 */
	@Autowired
	public void setJobInstanceDao(JobInstanceDao jobInstanceDao) {
		this.jobInstanceDao = jobInstanceDao;
	}

	@Autowired
	public void setJobExecutionDao(JobExecutionDao jobExecutionDao) {
		this.jobExecutionDao = jobExecutionDao;
	}

	@BeforeEach
	void onSetUpInTransaction() {
		// Create job.
		jobInstance = jobInstanceDao.createJobInstance(jobName, jobParameters);

		// Create an execution
		jobExecutionStartTime = LocalDateTime.now();
		jobExecution = new JobExecution(jobInstance, jobParameters);
		jobExecution.setStartTime(jobExecutionStartTime);
		jobExecution.setStatus(BatchStatus.STARTED);
		jobExecutionDao.saveJobExecution(jobExecution);
	}

	@Transactional
	@Test
	void testVersionIsNotNullForJob() {
		int version = jdbcTemplate.queryForObject(
				"select version from BATCH_JOB_INSTANCE where JOB_INSTANCE_ID=" + jobInstance.getId(), Integer.class);
		assertEquals(0, version);
	}

	@Transactional
	@Test
	void testVersionIsNotNullForJobExecution() {
		int version = jdbcTemplate.queryForObject(
				"select version from BATCH_JOB_EXECUTION where JOB_EXECUTION_ID=" + jobExecution.getId(),
				Integer.class);
		assertEquals(0, version);
	}

	@Transactional
	@Test
	void testFindNonExistentJob() {
		// No job should be found since it hasn't been created.
		JobInstance jobInstance = jobInstanceDao.getJobInstance("nonexistentJob", jobParameters);
		assertNull(jobInstance);
	}

	@Transactional
	@Test
	void testFindJob() {
		JobInstance instance = jobInstanceDao.getJobInstance(jobName, jobParameters);
		assertNotNull(instance);
		assertEquals(jobInstance, instance);
	}

	@Transactional
	@Test
	void testFindJobWithNullRuntime() {
		assertThrows(IllegalArgumentException.class, () -> jobInstanceDao.getJobInstance(null, null));
	}

	/**
	 * Test that ensures that if you create a job with a given name, then find a job with
	 * the same name, but other pieces of the identifier different, you get no result, not
	 * the existing one.
	 */
	@Transactional
	@Test
	void testCreateJobWithExistingName() {

		String scheduledJob = "ScheduledJob";
		jobInstanceDao.createJobInstance(scheduledJob, jobParameters);

		// Modifying the key should bring back a completely different
		// JobInstance
		JobParameters tempProps = new JobParametersBuilder().addString("job.key", "testKey1").toJobParameters();

		JobInstance instance;
		instance = jobInstanceDao.getJobInstance(scheduledJob, jobParameters);
		assertNotNull(instance);

		instance = jobInstanceDao.getJobInstance(scheduledJob, tempProps);
		assertNull(instance);

	}

	@Transactional
	@Test
	void testUpdateJobExecution() {

		jobExecution.setStatus(BatchStatus.COMPLETED);
		jobExecution.setExitStatus(ExitStatus.COMPLETED);
		jobExecution.setEndTime(LocalDateTime.now());
		jobExecutionDao.updateJobExecution(jobExecution);

		List<JobExecution> executions = jobExecutionDao.findJobExecutions(jobInstance);
		assertEquals(executions.size(), 1);
		validateJobExecution(jobExecution, executions.get(0));

	}

	@Transactional
	@Test
	void testSaveJobExecution() {

		List<JobExecution> executions = jobExecutionDao.findJobExecutions(jobInstance);
		assertEquals(executions.size(), 1);
		validateJobExecution(jobExecution, executions.get(0));
	}

	@Transactional
	@Test
	void testUpdateInvalidJobExecution() {

		// id is invalid
		JobExecution execution = new JobExecution(jobInstance, 29432L, jobParameters);
		execution.incrementVersion();
		assertThrows(NoSuchObjectException.class, () -> jobExecutionDao.updateJobExecution(execution));
	}

	@Transactional
	@Test
	void testUpdateNullIdJobExecution() {

		JobExecution execution = new JobExecution(jobInstance, jobParameters);
		assertThrows(IllegalArgumentException.class, () -> jobExecutionDao.updateJobExecution(execution));
	}

	@Transactional
	@Test
	void testJobWithSimpleJobIdentifier() {

		String testJob = "test";
		// Create job.
		jobInstance = jobInstanceDao.createJobInstance(testJob, jobParameters);

		List<Map<String, Object>> jobs = jdbcTemplate
			.queryForList("SELECT * FROM BATCH_JOB_INSTANCE where JOB_INSTANCE_ID=?", jobInstance.getId());
		assertEquals(1, jobs.size());
		assertEquals("test", jobs.get(0).get("JOB_NAME"));

	}

	@Transactional
	@Test
	void testJobWithDefaultJobIdentifier() {

		String testDefaultJob = "testDefault";
		// Create job.
		jobInstance = jobInstanceDao.createJobInstance(testDefaultJob, jobParameters);

		JobInstance instance = jobInstanceDao.getJobInstance(testDefaultJob, jobParameters);

		assertNotNull(instance);
	}

	@Transactional
	@Test
	void testFindJobExecutions() {

		List<JobExecution> results = jobExecutionDao.findJobExecutions(jobInstance);
		assertEquals(results.size(), 1);
		validateJobExecution(jobExecution, results.get(0));
	}

	private void validateJobExecution(JobExecution lhs, JobExecution rhs) {

		// equals operator only checks id
		assertEquals(lhs, rhs);
		assertEquals(lhs.getStartTime(), rhs.getStartTime());
		assertEquals(lhs.getEndTime(), rhs.getEndTime());
		assertEquals(lhs.getStatus(), rhs.getStatus());
		assertEquals(lhs.getExitStatus(), rhs.getExitStatus());
	}

	@Transactional
	@Test
	void testGetLastJobExecution() {
		JobExecution lastExecution = new JobExecution(jobInstance, jobParameters);
		lastExecution.setStatus(BatchStatus.STARTED);

		int JUMP_INTO_FUTURE = 1000; // makes sure start time is 'greatest'
		lastExecution.setCreateTime(LocalDateTime.now().plus(JUMP_INTO_FUTURE, ChronoUnit.MILLIS));
		jobExecutionDao.saveJobExecution(lastExecution);

		assertEquals(lastExecution, jobExecutionDao.getLastJobExecution(jobInstance));
		assertNotNull(lastExecution.getJobParameters());
		assertEquals("jobKey", lastExecution.getJobParameters().getString("job.key"));
	}

	/**
	 * Trying to create instance twice for the same job+parameters causes error
	 */
	@Transactional
	@Test
	void testCreateDuplicateInstance() {

		jobParameters = new JobParameters();

		jobInstanceDao.createJobInstance(jobName, jobParameters);

		assertThrows(IllegalStateException.class, () -> jobInstanceDao.createJobInstance(jobName, jobParameters));
	}

	@Transactional
	@Test
	void testCreationAddsVersion() {

		jobInstance = jobInstanceDao.createJobInstance("testCreationAddsVersion", new JobParameters());

		assertNotNull(jobInstance.getVersion());
	}

	@Transactional
	@Test
	void testSaveAddsVersionAndId() {

		JobExecution jobExecution = new JobExecution(jobInstance, jobParameters);

		assertNull(jobExecution.getId());
		assertNull(jobExecution.getVersion());

		jobExecutionDao.saveJobExecution(jobExecution);

		assertNotNull(jobExecution.getId());
		assertNotNull(jobExecution.getVersion());
	}

	@Transactional
	@Test
	void testUpdateIncrementsVersion() {
		int version = jobExecution.getVersion();

		jobExecutionDao.updateJobExecution(jobExecution);

		assertEquals(version + 1, jobExecution.getVersion().intValue());
	}

}
