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

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobSupport;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.JobParametersBuilder;
import org.springframework.batch.core.repository.NoSuchBatchDomainObjectException;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;
import org.springframework.util.ClassUtils;

/**
 * @author Dave Syer
 *
 */
public abstract class AbstractJobDaoTests extends
		AbstractTransactionalDataSourceSpringContextTests {

	protected JobInstanceDao jobInstanceDao;
	
	protected JobExecutionDao jobExecutionDao;

	protected JobParameters jobParameters = new JobParametersBuilder().addString("job.key", "jobKey").toJobParameters();
	
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
	public void setJobInstanceDao(JobInstanceDao jobInstanceDao) {
		this.jobInstanceDao = jobInstanceDao;
	}
	
	public void setJobExecutionDao(JobExecutionDao jobExecutionDao) {
		this.jobExecutionDao = jobExecutionDao;
	}

	protected void onSetUpInTransaction() throws Exception {
//		jobRuntimeInformation = new ScheduledJobIdentifier("Job1", "TestStream",
//				new SimpleDateFormat("yyyyMMdd").parse("20070505"));

		job = new JobSupport("Job1");
		
		// Create job.
		jobInstance = jobInstanceDao.createJobInstance(job.getName(), jobParameters);

		// Create an execution
		jobExecutionStartTime = new Date(System.currentTimeMillis());
		jobExecution = new JobExecution(jobInstance);
		jobExecution.setStartTime(jobExecutionStartTime);
		jobExecution.setStatus(BatchStatus.STARTED);
		jobExecutionDao.saveJobExecution(jobExecution);
		jobInstance.setLastExecution(jobExecution);
		jobInstanceDao.updateJobInstance(jobInstance);
	}

	public void testVersionIsNotNullForJob() throws Exception {
		int version = jdbcTemplate
				.queryForInt("select version from BATCH_JOB_INSTANCE where JOB_INSTANCE_ID="
						+ jobInstance.getId());
		assertEquals(0, version);
	}

	public void testVersionIsNotNullForJobExecution() throws Exception {
		int version = jdbcTemplate
				.queryForInt("select version from BATCH_JOB_EXECUTION where JOB_EXECUTION_ID="
						+ jobExecution.getId());
		assertEquals(0, version);
	}

	public void testFindNonExistentJob() {
		// No job should be found since it hasn't been created.
		List jobs = jobInstanceDao.findJobInstances("nonexistentJob", jobParameters);
		assertTrue(jobs.size() == 0);
	}

	public void testFindJob() {

		List jobs = jobInstanceDao.findJobInstances(job.getName(), jobParameters);
		assertTrue(jobs.size() == 1);
		JobInstance tempJob = (JobInstance) jobs.get(0);
		assertTrue(jobInstance.equals(tempJob));
		assertEquals(jobParameters, tempJob.getJobParameters());
	}

	public void testFindJobWithNullRuntime() {

		try {
			jobInstanceDao.findJobInstances(null, null);
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
		
		jobInstanceDao.createJobInstance("ScheduledJob", jobParameters);

		// Modifying the key should bring back a completely different
		// JobInstance
		JobParameters tempProps = new JobParametersBuilder().addString("job.key", "testKey1")
			.toJobParameters();

		List jobs;
		jobs = jobInstanceDao.findJobInstances("ScheduledJob", jobParameters);
		assertEquals(1, jobs.size());
		JobInstance jobInstance = (JobInstance) jobs.get(0);
		assertEquals(jobParameters, jobInstance.getJobParameters());

		jobs = jobInstanceDao.findJobInstances("ScheduledJob", tempProps);
		assertEquals(0, jobs.size());

	}

	public void testUpdateJob() {
		// Update the returned job with a new status
		JobExecution newExecution = new JobExecution(jobInstance);
		jobExecutionDao.saveJobExecution(newExecution);
		jobInstance.setLastExecution(newExecution);
		jobInstanceDao.updateJobInstance(jobInstance);

		// The job just updated should be found, with the saved status.
		List jobs = jobInstanceDao.findJobInstances(job.getName(), jobParameters);
		assertTrue(jobs.size() == 1);
		JobInstance tempJob = (JobInstance) jobs.get(0);
		assertTrue(jobInstance.equals(tempJob));
		assertEquals(newExecution, tempJob.getLastExecution());
	}
	
	public void testGetJobExecution(){
		
		JobExecution tempExecution = jobExecutionDao.getJobExecution(jobExecution.getId());
		assertEquals(jobExecution, tempExecution);
	}
	
	public void testJobInstanceLastExecution(){
		//ensure the last execution id is being stored
		JobExecution lastJobExecution = jobExecutionDao.getJobExecution(jobInstance.getLastExecution().getId());
		assertEquals(lastJobExecution, jobExecution);
	}

	public void testUpdateJobWithNullId() {

		
		try {
			JobInstance testJob = new JobInstance(null, null);
			jobInstanceDao.updateJobInstance(testJob);
			fail();
		} catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testUpdateNullJob() {

		JobInstance testJob = null;
		try {
			jobInstanceDao.updateJobInstance(testJob);
		} catch (IllegalArgumentException ex) {
			// expected
		}
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

	public void testSaveJobExecution(){

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
		} catch (NoSuchBatchDomainObjectException ex) {
			// expected
		}
	}

	public void testUpdateNullIdJobExection() {

		JobExecution execution = new JobExecution(jobInstance);
		try {
			jobExecutionDao.updateJobExecution(execution);
			fail();
		} catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testIncrementExecutionCount() {

		// 1 JobExection already added in setup
		assertEquals(jobExecutionDao.getJobExecutionCount(jobInstance.getId()), 1);

		// Save new JobExecution for same job
		JobExecution testJobExecution = new JobExecution(jobInstance);
		jobExecutionDao.saveJobExecution(testJobExecution);
		// JobExecutionCount should be incremented by 1
		assertEquals(jobExecutionDao.getJobExecutionCount(jobInstance.getId()), 2);
	}

	public void testZeroExecutionCount() {

		JobInstance testJob = jobInstanceDao.createJobInstance("test", new JobParameters());
		// no jobExecutions saved for new job, count should be 0
		assertEquals(jobExecutionDao.getJobExecutionCount(testJob.getId()), 0);
	}

	public void testJobWithSimpleJobIdentifier() throws Exception {

		// Create job.
		jobInstance = jobInstanceDao.createJobInstance("test", jobParameters);

		List jobs = jdbcTemplate.queryForList(
				"SELECT * FROM BATCH_JOB_INSTANCE where JOB_INSTANCE_ID=?", new Object[] { jobInstance
						.getId() });
		assertEquals(1, jobs.size());
		assertEquals("test", ((Map) jobs.get(0)).get("JOB_NAME"));

	}

	public void testJobWithDefaultJobIdentifier() throws Exception {
		// Create job.
		jobInstance = jobInstanceDao.createJobInstance("testDefault", jobParameters);
		
		List jobs = jobInstanceDao.findJobInstances("testDefault", jobParameters); 
			
		assertEquals(1, jobs.size());
		assertEquals(jobParameters.getString("job.key"), ((JobInstance) jobs.get(0))
				.getJobParameters().getString("job.key"));

	}

	public void testFindJobExecutions(){

		List results = jobExecutionDao.findJobExecutions(jobInstance);
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
