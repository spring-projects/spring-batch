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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;

import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.repository.NoSuchBatchDomainObjectException;
import org.springframework.batch.execution.runtime.ScheduledJobIdentifier;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;
import org.springframework.util.ClassUtils;

public class SqlJobDaoTests extends AbstractTransactionalDataSourceSpringContextTests {

	private static final String GET_JOB_EXECUTION = "SELECT JOB_ID, START_TIME, END_TIME, STATUS from " +
			"BATCH_JOB_EXECUTION where ID = ?";
	
	protected JobDao jobDao;
	
	protected ScheduledJobIdentifier jobRuntimeInformation;
	
	protected JobInstance job;
	
	protected JobExecution jobExecution;
	
	protected Timestamp jobExecutionStartTime = new Timestamp(System.currentTimeMillis());
	
	protected String[] getConfigLocations() {
		return new String[] { ClassUtils.addResourcePathToPackagePath(getClass(), "sql-dao-test.xml") };
	}

	/*
	 * Because AbstractTransactionalSpringContextTests is used, this method will
	 * be called by Spring to set the JobRepository.
	 */
	public void setJobRepositoryDao(JobDao jobRepositoryDao) {
		this.jobDao = jobRepositoryDao;
	}
		
	protected void onSetUpInTransaction() throws Exception {
		jobRuntimeInformation = new ScheduledJobIdentifier("Job1");
		jobRuntimeInformation.setName("Job1");
		jobRuntimeInformation.setJobStream("TestStream");
		jobRuntimeInformation.setJobRun(1);
		jobRuntimeInformation.setScheduleDate(new SimpleDateFormat("yyyyMMdd").parse("20070505"));
		
		// Create job.
		job = jobDao.createJob(jobRuntimeInformation);
		
		// Create an execution
		jobExecutionStartTime = new Timestamp(System.currentTimeMillis());
		jobExecution = new JobExecution(job.getId());
		jobExecution.setStartTime(jobExecutionStartTime);
		jobExecution.setStatus(BatchStatus.STARTED);
		jobDao.save(jobExecution);
	}
	
	public void testVersionIsNotNullForJob() throws Exception {
		int version = jdbcTemplate.queryForInt("select version from BATCH_JOB where ID="+job.getId());
		assertEquals(0, version);
	}
	
	public void testVersionIsNotNullForJobExecution() throws Exception {
		int version = jdbcTemplate.queryForInt("select version from BATCH_JOB_EXECUTION where ID="+jobExecution.getId());
		assertEquals(0, version);
	}

	public void testFindNonExistentJob(){
		// No job should be found since it hasn't been created.
		List jobs = jobDao.findJobs(new ScheduledJobIdentifier("Job2"));
		assertTrue(jobs.size() == 0);
	}
	
	public void testFindJob(){
		
		List jobs = jobDao.findJobs(jobRuntimeInformation);
		assertTrue(jobs.size() == 1);
		JobInstance tempJob = (JobInstance) jobs.get(0);
		assertTrue(job.equals(tempJob));
	}
	
	public void testFindJobWithNullRuntime(){
		
		ScheduledJobIdentifier runtimeInformation = null;
		
		try{
			jobDao.findJobs(runtimeInformation);
			fail();
		}catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	public void testUpdateJob(){
		// Update the returned job with a new status
		job.setStatus(BatchStatus.COMPLETED);
		jobDao.update(job);
		
		// The job just updated should be found, with the saved status.
		List jobs = jobDao.findJobs(jobRuntimeInformation);
		assertTrue(jobs.size() == 1);
		JobInstance tempJob = (JobInstance) jobs.get(0);
		assertTrue(job.equals(tempJob));
		assertEquals(tempJob.getStatus(), BatchStatus.COMPLETED);
	}
	
	public void testUpdateJobWithNullId(){
		
		JobInstance testJob = new JobInstance(null);
		try{
			jobDao.update(testJob);
			fail();
		}catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	public void testUpdateNullJob(){
		
		JobInstance testJob = null;	
		try{
			jobDao.update(testJob);
		}catch(IllegalArgumentException ex){
			//expected
		}
	}

	public void testUpdateJobExecution() {

		jobExecution.setStatus(BatchStatus.COMPLETED);
		jobExecution.setEndTime(new Timestamp(System.currentTimeMillis()));
		jobDao.update(jobExecution);
		
		List executions = retrieveJobExecution(jobExecution.getId());
		assertEquals(executions.size(), 1);
		assertEquals(jobExecution, ((JobExecution)executions.get(0)));
	}
	
	public void testUpdateInvalidJobExecution(){
		
		JobExecution execution = new JobExecution(job.getId());
		//id is invalid
		execution.setId(new Long(29432));
		try{
			jobDao.update(execution);
			fail();
		}catch(NoSuchBatchDomainObjectException ex){
			//expected
		}
	}
	
	public void testUpdateNullIdJobExection(){
		
		JobExecution execution = new JobExecution(job.getId());
		try{
			jobDao.update(execution);
			fail();
		}catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	public void testIncrementExecutionCount(){
		
		// 1 JobExection already added in setup
		assertEquals(jobDao.getJobExecutionCount(job.getId()), 1);
		
		// Save new JobExecution for same job
		JobExecution testJobExecution = new JobExecution(job.getId());
		jobDao.save(testJobExecution);
		//JobExecutionCount should be incremented by 1
		assertEquals(jobDao.getJobExecutionCount(job.getId()), 2);
	}
	
	public void testZeroExecutionCount(){
		
		JobInstance testJob = jobDao.createJob(new ScheduledJobIdentifier("TestJob"));
		//no jobExecutions saved for new job, count should be 0
		assertEquals(jobDao.getJobExecutionCount(testJob.getId()), 0);
	}
	
	private List retrieveJobExecution(final Long id){
		
		RowMapper rowMapper = new RowMapper(){
			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
				
				JobExecution execution = new JobExecution(new Long(rs.getLong(1)));
				execution.setStartTime(rs.getTimestamp(2));
				execution.setEndTime(rs.getTimestamp(3));
				execution.setStatus(BatchStatus.getStatus(rs.getString(4)));
				execution.setId(id);
				
				return execution;
			}
		};
		
		return jdbcTemplate.query(GET_JOB_EXECUTION, new Object[]{id}, rowMapper);
	}

}
