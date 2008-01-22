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

package org.springframework.batch.execution.repository.dao;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobInstanceProperties;
import org.springframework.batch.core.domain.JobInstancePropertiesBuilder;
import org.springframework.batch.core.repository.NoSuchBatchDomainObjectException;
import org.springframework.batch.core.runtime.SimpleJobIdentifier;
import org.springframework.batch.execution.runtime.DefaultJobIdentifier;
import org.springframework.batch.execution.runtime.ScheduledJobIdentifier;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;
import org.springframework.util.ClassUtils;

/**
 * @author Dave Syer
 *
 */
public abstract class AbstractJobDaoTests extends
		AbstractTransactionalDataSourceSpringContextTests {

	protected JobDao jobDao;

	protected JobInstanceProperties jobInstanceProperties = new JobInstancePropertiesBuilder().addString("job.key", "jobKey").toJobParameters();
	
	protected JobInstance jobInstance;
	
	protected Job job;

	protected JobExecution jobExecution;

	protected Date jobExecutionStartTime = new Date(System
			.currentTimeMillis());

	protected String[] getConfigLocations() {
		return new String[] { ClassUtils.addResourcePathToPackagePath(
				getClass(), "sql-dao-test.xml") };
	}

	/*
	 * Because AbstractTransactionalSpringContextTests is used, this method will
	 * be called by Spring to set the JobRepository.
	 */
	public void setJobRepositoryDao(JobDao jobRepositoryDao) {
		this.jobDao = jobRepositoryDao;
	}

	protected void onSetUpInTransaction() throws Exception {
//		jobRuntimeInformation = new ScheduledJobIdentifier("Job1", "TestStream",
//				new SimpleDateFormat("yyyyMMdd").parse("20070505"));

		job = new Job("Job1");
		
		// Create job.
		jobInstance = jobDao.createJobInstance(job.getName(), jobInstanceProperties);

		// Create an execution
		jobExecutionStartTime = new Date(System.currentTimeMillis());
		jobExecution = new JobExecution(jobInstance);
		jobExecution.setStartTime(jobExecutionStartTime);
		jobExecution.setStatus(BatchStatus.STARTED);
		jobDao.save(jobExecution);
	}

	public void testVersionIsNotNullForJob() throws Exception {
		int version = jdbcTemplate
				.queryForInt("select version from BATCH_JOB_INSTANCE where ID="
						+ jobInstance.getId());
		assertEquals(0, version);
	}

	public void testVersionIsNotNullForJobExecution() throws Exception {
		int version = jdbcTemplate
				.queryForInt("select version from BATCH_JOB_EXECUTION where ID="
						+ jobExecution.getId());
		assertEquals(0, version);
	}

	public void testFindNonExistentJob() {
		// No job should be found since it hasn't been created.
		List jobs = jobDao.findJobInstances("nonexistentJob", jobInstanceProperties);
		assertTrue(jobs.size() == 0);
	}

	public void testFindJob() {

		List jobs = jobDao.findJobInstances(job.getName(), jobInstanceProperties);
		assertTrue(jobs.size() == 1);
		JobInstance tempJob = (JobInstance) jobs.get(0);
		assertTrue(jobInstance.equals(tempJob));
		assertEquals(jobInstanceProperties, tempJob.getJobInstanceProperties());
	}

	public void testFindJobWithNullRuntime() {

		try {
			jobDao.findJobInstances(null, null);
			fail();
		} catch (IllegalArgumentException ex) {
			// expected
		}
	}

	/**
	 * Test that ensures that if you create a job with a given name, then find a
	 * job with the same name, but other pieces of the identifier different, you
	 * get no result, not the existing one.
	 */
	public void testCreateJobWithExistingName() {
		
		jobDao.createJobInstance("ScheduledJob", jobInstanceProperties);

		// Modifying the key should bring back a completely different
		// JobInstance
		JobInstanceProperties tempProps = new JobInstancePropertiesBuilder().addString("job.key", "testKey1")
			.toJobParameters();

		List jobs;
		jobs = jobDao.findJobInstances("ScheduledJob", jobInstanceProperties);
		assertEquals(1, jobs.size());
		JobInstance jobInstance = (JobInstance) jobs.get(0);
		assertEquals(jobInstanceProperties, jobInstance.getJobInstanceProperties());

		jobs = jobDao.findJobInstances("ScheduledJob", tempProps);
		assertEquals(0, jobs.size());

	}

	public void testUpdateJob() {
		// Update the returned job with a new status
		jobInstance.setStatus(BatchStatus.COMPLETED);
		jobDao.update(jobInstance);

		// The job just updated should be found, with the saved status.
		List jobs = jobDao.findJobInstances(job.getName(), jobInstanceProperties);
		assertTrue(jobs.size() == 1);
		JobInstance tempJob = (JobInstance) jobs.get(0);
		assertTrue(jobInstance.equals(tempJob));
		assertEquals(tempJob.getStatus(), BatchStatus.COMPLETED);
	}

	public void testUpdateJobWithNullId() {

		
		try {
			JobInstance testJob = new JobInstance(null, null);
			jobDao.update(testJob);
			fail();
		} catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testUpdateNullJob() {

		JobInstance testJob = null;
		try {
			jobDao.update(testJob);
		} catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testUpdateJobExecution() {

		jobExecution.setStatus(BatchStatus.COMPLETED);
		jobExecution.setExitStatus(ExitStatus.FINISHED);
		jobExecution.setEndTime(new Date(System.currentTimeMillis()));
		jobDao.update(jobExecution);

		List executions = jobDao.findJobExecutions(jobInstance);
		assertEquals(executions.size(), 1);
		validateJobExecution(jobExecution, (JobExecution) executions.get(0));

	}

	public void testSaveJobExecution(){

		List executions = jobDao.findJobExecutions(jobInstance);
		assertEquals(executions.size(), 1);
		validateJobExecution(jobExecution, (JobExecution) executions.get(0));
	}

	public void testUpdateInvalidJobExecution() {

		// id is invalid
		JobExecution execution = new JobExecution(jobInstance, new Long(29432));
		try {
			jobDao.update(execution);
			fail("Expected NoSuchBatchDomainObjectException");
		} catch (NoSuchBatchDomainObjectException ex) {
			// expected
		}
	}

	public void testUpdateNullIdJobExection() {

		JobExecution execution = new JobExecution(jobInstance);
		try {
			jobDao.update(execution);
			fail();
		} catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testIncrementExecutionCount() {

		// 1 JobExection already added in setup
		assertEquals(jobDao.getJobExecutionCount(jobInstance.getId()), 1);

		// Save new JobExecution for same job
		JobExecution testJobExecution = new JobExecution(jobInstance);
		jobDao.save(testJobExecution);
		// JobExecutionCount should be incremented by 1
		assertEquals(jobDao.getJobExecutionCount(jobInstance.getId()), 2);
	}

	public void testZeroExecutionCount() {

		JobInstance testJob = jobDao.createJobInstance("test", new JobInstanceProperties());
		// no jobExecutions saved for new job, count should be 0
		assertEquals(jobDao.getJobExecutionCount(testJob.getId()), 0);
	}

	public void testJobWithSimpleJobIdentifier() throws Exception {
		SimpleJobIdentifier jobIdentifier = new SimpleJobIdentifier("Job1");

		// Create job.
		jobInstance = jobDao.createJobInstance("test", jobInstanceProperties);

		List jobs = jdbcTemplate.queryForList(
				"SELECT * FROM BATCH_JOB_INSTANCE where ID=?", new Object[] { jobInstance
						.getId() });
		assertEquals(1, jobs.size());
		assertEquals("test", ((Map) jobs.get(0)).get("JOB_NAME"));

	}

	public void testJobWithDefaultJobIdentifier() throws Exception {
		// Create job.
		jobInstance = jobDao.createJobInstance("testDefault", jobInstanceProperties);
		
		List jobs = jobDao.findJobInstances("testDefault", jobInstanceProperties); 
			
		assertEquals(1, jobs.size());
		assertEquals(jobInstanceProperties.getString("job.key"), ((JobInstance) jobs.get(0))
				.getJobInstanceProperties().getString("job.key"));

	}

	public void testFindJobExecutions(){

		List results = jobDao.findJobExecutions(jobInstance);
		assertEquals(results.size(), 1);
		validateJobExecution(jobExecution, (JobExecution)results.get(0));
	}
	
	public void testFindJobsWithProperties() throws Exception{
		
		
	}

	private void validateJobExecution(JobExecution lhs, JobExecution rhs){

		//equals operator only checks id
		assertEquals(lhs, rhs);
		assertEquals(lhs.getStartTime(), rhs.getStartTime());
		assertEquals(lhs.getEndTime(), rhs.getEndTime());
		assertEquals(lhs.getStatus(), rhs.getStatus());
		assertEquals(lhs.getExitStatus(), rhs.getExitStatus());
	}

}
