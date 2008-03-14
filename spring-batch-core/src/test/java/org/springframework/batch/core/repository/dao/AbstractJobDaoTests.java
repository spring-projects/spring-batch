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

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.NoSuchObjectException;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;
import org.springframework.util.ClassUtils;

/**
 * @author Dave Syer
 * 
 */
public abstract class AbstractJobDaoTests extends AbstractTransactionalDataSourceSpringContextTests {

	protected JobInstanceDao jobInstanceDao;

	protected JobExecutionDao jobExecutionDao;

	protected JobParameters jobParameters = new JobParametersBuilder().addString("job.key", "jobKey").addLong("long",
			new Long(1)).addDate("date", new Date(7)).addDouble("double", new Double(7.7)).toJobParameters();

	protected JobInstance jobInstance;

	protected Job job;

	protected JobExecution jobExecution;

	protected Date jobExecutionStartTime = new Date(System.currentTimeMillis());

	protected String[] getConfigLocations() {
		return new String[] { ClassUtils.addResourcePathToPackagePath(getClass(), "sql-dao-test.xml") };
	}

	/**
	 * Because AbstractTransactionalSpringContextTests is used, this method will
	 * be called by Spring to set the JobRepository.
	 */
	public void setJobInstanceDao(JobInstanceDao jobInstanceDao) {
		this.jobInstanceDao = jobInstanceDao;
	}

	public void setJobExecutionDao(JobExecutionDao jobExecutionDao) {
		this.jobExecutionDao = jobExecutionDao;
	}

	protected void onSetUpInTransaction() throws Exception {

		job = new JobSupport("Job1");

		// Create job.
		jobInstance = jobInstanceDao.createJobInstance(job, jobParameters);

		// Create an execution
		jobExecutionStartTime = new Date(System.currentTimeMillis());
		jobExecution = new JobExecution(jobInstance);
		jobExecution.setStartTime(jobExecutionStartTime);
		jobExecution.setStatus(BatchStatus.STARTED);
		jobExecutionDao.saveJobExecution(jobExecution);
	}

	public void testVersionIsNotNullForJob() throws Exception {
		int version = jdbcTemplate.queryForInt("select version from BATCH_JOB_INSTANCE where JOB_INSTANCE_ID="
				+ jobInstance.getId());
		assertEquals(0, version);
	}

	public void testVersionIsNotNullForJobExecution() throws Exception {
		int version = jdbcTemplate.queryForInt("select version from BATCH_JOB_EXECUTION where JOB_EXECUTION_ID="
				+ jobExecution.getId());
		assertEquals(0, version);
	}

	public void testFindNonExistentJob() {
		// No job should be found since it hasn't been created.
		JobInstance jobInstance = jobInstanceDao.getJobInstance(new JobSupport("nonexistentJob"), jobParameters);
		assertNull(jobInstance);
	}

	public void testFindJob() {

		JobInstance instance = jobInstanceDao.getJobInstance(job, jobParameters);
		assertNotNull(instance);
		assertTrue(jobInstance.equals(instance));
		assertEquals(jobParameters, instance.getJobParameters());
	}

	public void testFindJobWithNullRuntime() {

		try {
			jobInstanceDao.getJobInstance(null, null);
			fail();
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	/**
	 * Test that ensures that if you create a job with a given name, then find a
	 * job with the same name, but other pieces of the identifier different, you
	 * get no result, not the existing one.
	 */
	public void testCreateJobWithExistingName() {

		Job scheduledJob = new JobSupport("ScheduledJob");
		jobInstanceDao.createJobInstance(scheduledJob, jobParameters);

		// Modifying the key should bring back a completely different
		// JobInstance
		JobParameters tempProps = new JobParametersBuilder().addString("job.key", "testKey1").toJobParameters();

		JobInstance instance;
		instance = jobInstanceDao.getJobInstance(scheduledJob, jobParameters);
		assertNotNull(instance);
		assertEquals(jobParameters, instance.getJobParameters());

		instance = jobInstanceDao.getJobInstance(scheduledJob, tempProps);
		assertNull(instance);

	}

	public void testUpdateJobExecution() {

		jobExecution.setStatus(BatchStatus.COMPLETED);
		jobExecution.setExitStatus(ExitStatus.FINISHED);
		jobExecution.setEndTime(new Date(System.currentTimeMillis()));
		jobExecutionDao.updateJobExecution(jobExecution);

		List executions = jobExecutionDao.findJobExecutions(jobInstance);
		assertEquals(executions.size(), 1);
		validateJobExecution(jobExecution, (JobExecution) executions.get(0));

	}

	public void testSaveJobExecution() {

		List executions = jobExecutionDao.findJobExecutions(jobInstance);
		assertEquals(executions.size(), 1);
		validateJobExecution(jobExecution, (JobExecution) executions.get(0));
	}

	public void testUpdateInvalidJobExecution() {

		// id is invalid
		JobExecution execution = new JobExecution(jobInstance, new Long(29432));
		try {
			jobExecutionDao.updateJobExecution(execution);
			fail("Expected NoSuchBatchDomainObjectException");
		}
		catch (NoSuchObjectException ex) {
			// expected
		}
	}

	public void testUpdateNullIdJobExection() {

		JobExecution execution = new JobExecution(jobInstance);
		try {
			jobExecutionDao.updateJobExecution(execution);
			fail();
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testIncrementExecutionCount() {

		// 1 JobExection already added in setup
		assertEquals(jobExecutionDao.getJobExecutionCount(jobInstance), 1);

		// Save new JobExecution for same job
		JobExecution testJobExecution = new JobExecution(jobInstance);
		jobExecutionDao.saveJobExecution(testJobExecution);
		// JobExecutionCount should be incremented by 1
		assertEquals(jobExecutionDao.getJobExecutionCount(jobInstance), 2);
	}

	public void testZeroExecutionCount() {

		JobInstance testJob = jobInstanceDao.createJobInstance(new JobSupport("test"), new JobParameters());
		// no jobExecutions saved for new job, count should be 0
		assertEquals(jobExecutionDao.getJobExecutionCount(testJob), 0);
	}

	public void testJobWithSimpleJobIdentifier() throws Exception {

		Job testJob = new JobSupport("test");
		// Create job.
		jobInstance = jobInstanceDao.createJobInstance(testJob, jobParameters);

		List jobs = jdbcTemplate.queryForList("SELECT * FROM BATCH_JOB_INSTANCE where JOB_INSTANCE_ID=?",
				new Object[] { jobInstance.getId() });
		assertEquals(1, jobs.size());
		assertEquals("test", ((Map) jobs.get(0)).get("JOB_NAME"));

	}

	public void testJobWithDefaultJobIdentifier() throws Exception {

		Job testDefaultJob = new JobSupport("testDefault");
		// Create job.
		jobInstance = jobInstanceDao.createJobInstance(testDefaultJob, jobParameters);

		JobInstance instance = jobInstanceDao.getJobInstance(testDefaultJob, jobParameters);

		assertNotNull(instance);
		assertEquals(jobParameters.getString("job.key"), instance.getJobParameters().getString(
				"job.key"));

	}

	public void testFindJobExecutions() {

		List results = jobExecutionDao.findJobExecutions(jobInstance);
		assertEquals(results.size(), 1);
		validateJobExecution(jobExecution, (JobExecution) results.get(0));
	}

	public void testFindJobsWithProperties() throws Exception {

	}

	private void validateJobExecution(JobExecution lhs, JobExecution rhs) {

		// equals operator only checks id
		assertEquals(lhs, rhs);
		assertEquals(lhs.getStartTime(), rhs.getStartTime());
		assertEquals(lhs.getEndTime(), rhs.getEndTime());
		assertEquals(lhs.getStatus(), rhs.getStatus());
		assertEquals(lhs.getExitStatus(), rhs.getExitStatus());
	}

	public void testGetLastJobExecution() {
		JobExecution lastExecution = new JobExecution(jobInstance);
		lastExecution.setStatus(BatchStatus.STARTED);

		int JUMP_INTO_FUTURE = 1000; // makes sure start time is 'greatest'
		lastExecution.setStartTime(new Date(System.currentTimeMillis() + JUMP_INTO_FUTURE));
		jobExecutionDao.saveJobExecution(lastExecution);

		assertEquals(lastExecution, jobExecutionDao.getLastJobExecution(jobInstance));
	}
	
	/**
	 * Trying to create instance twice for the same job+parameters causes error
	 */
	public void testCreateDuplicateInstance() {
		
		jobParameters = new JobParameters();
		
		jobInstanceDao.createJobInstance(job, jobParameters);
		
		try {
			jobInstanceDao.createJobInstance(job, jobParameters);
			fail();
		}
		catch (IllegalStateException e) {
			// expected
		}
	}
	
	public void testCreationAddsVersion() {
		
		jobInstance = jobInstanceDao.createJobInstance(new JobSupport("testVersion"), new JobParameters());
		
		assertNotNull(jobInstance.getVersion());
	}
	
	public void testSaveAddsVersionAndId() {
		
		JobExecution jobExecution = new JobExecution(jobInstance);
		
		assertNull(jobExecution.getId());
		assertNull(jobExecution.getVersion());
		
		jobExecutionDao.saveJobExecution(jobExecution);
		
		assertNotNull(jobExecution.getId());
		assertNotNull(jobExecution.getVersion());
	}
	
	public void testUpdateIncrementsVersion() {
		int version = jobExecution.getVersion().intValue();
		
		jobExecutionDao.updateJobExecution(jobExecution);
		
		assertEquals(version + 1, jobExecution.getVersion().intValue());
	}
}
