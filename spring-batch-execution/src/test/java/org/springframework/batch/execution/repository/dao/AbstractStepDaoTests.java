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

import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.JobSupport;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;
import org.springframework.util.ClassUtils;

/**
 * Tests for step persistence (StepInstanceDao and StepExecutionDao). Because it is very reasonable to assume that there is a
 * foreign key constraint on the JobId of a step, the JobDao is used to create
 * jobs, to have an id for creating steps.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 */
public abstract class AbstractStepDaoTests extends AbstractTransactionalDataSourceSpringContextTests {

	protected JobInstanceDao jobInstanceDao;
	
	protected StepExecutionDao stepExecutionDao;
	
	protected JobExecutionDao jobExecutionDao;

	protected JobInstance jobInstance;

	protected String step1;

	protected String step2;

	protected StepExecution stepExecution;

	protected JobExecution jobExecution;

	protected JobParameters jobParameters = new JobParameters();
	
	protected ExecutionContext executionContext;

	public void setJobInstanceDao(JobInstanceDao jobInstanceDao) {
		this.jobInstanceDao = jobInstanceDao;
	}

	public void setStepExecutionDao(StepExecutionDao stepExecutionDao) {
		this.stepExecutionDao = stepExecutionDao;
	}

	public void setJobExecutionDao(JobExecutionDao jobExecutionDao) {
		this.jobExecutionDao = jobExecutionDao;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.test.AbstractSingleSpringContextTests#getConfigLocations()
	 */
	protected String[] getConfigLocations() {
		return new String[] { ClassUtils.addResourcePathToPackagePath(getClass(), "sql-dao-test.xml") };
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.test.AbstractTransactionalSpringContextTests#onSetUpInTransaction()
	 */
	protected void onSetUpInTransaction() throws Exception {
		Job job = new JobSupport("TestJob");
		jobInstance = jobInstanceDao.createJobInstance(job.getName(), jobParameters);
		step1 = "TestStep1";
		step2 = "TestStep2";
		jobExecution = new JobExecution(jobInstance);
		jobExecutionDao.saveJobExecution(jobExecution);

		stepExecution = new StepExecution(step1, jobExecution, new Long(1));
		stepExecution.setStatus(BatchStatus.STARTED);
		stepExecution.setStartTime(new Date(System.currentTimeMillis()));
		stepExecutionDao.saveStepExecution(stepExecution);
//		step1.setLastExecution(stepExecution);
		//stepInstanceDao.updateStepInstance(step1);
		
		executionContext = new ExecutionContext();
		executionContext.putString("1", "testString1");
		executionContext.putString("2", "testString2");
		executionContext.putLong("3", 3);
		executionContext.putDouble("4", 4.4);
		

	}

	public void testVersionIsNotNullForStepExecution() throws Exception {
		int version = jdbcTemplate.queryForInt("select version from BATCH_STEP_EXECUTION where STEP_EXECUTION_ID="
				+ stepExecution.getId());
		assertEquals(0, version);
	}

	public void testUpdateStepWithExecutionContext() {
		stepExecution.setExecutionContext(executionContext);
		stepExecutionDao.saveExecutionContext(stepExecution);
//		StepInstance tempStep = stepInstanceDao.findStepInstance(jobInstance, step1.getName());
		ExecutionContext tempAttributes = stepExecutionDao.findExecutionContext(stepExecution);
//		assertEquals(tempStep, step1);
		assertEquals(executionContext, tempAttributes);
	}
//	TODO update
//	public void testSaveStepExecution() {
//
//		StepExecution execution = new StepExecution(step2, jobExecution, null);
//		execution.setStatus(BatchStatus.STARTED);
//		execution.setStartTime(new Date(System.currentTimeMillis()));
//		execution.setExecutionContext(new ExecutionContext(PropertiesConverter.stringToProperties("key1=0,key2=5")));
//		execution.setExitStatus(new ExitStatus(false, ExitStatusExceptionClassifier.FATAL_EXCEPTION,
//				"java.lang.Exception"));
//		stepExecutionDao.saveStepExecution(execution);
//		List executions = stepExecutionDao.findStepExecutions(step2, null);
//		assertEquals(1, executions.size());
//		StepExecution tempExecution = (StepExecution) executions.get(0);
//		assertEquals(execution, tempExecution);
//		assertEquals(execution.getExecutionContext().getString("key1"), tempExecution.getExecutionContext().getString("key1"));
//		assertEquals(execution.getExitStatus(), tempExecution.getExitStatus());
//	}
//
//	public void testUpdateStepExecution() {
//
//		stepExecution.setStatus(BatchStatus.COMPLETED);
//		stepExecution.setEndTime(new Date(System.currentTimeMillis()));
//		stepExecution.setCommitCount(5);
//		stepExecution.setTaskCount(5);
//		stepExecution.setExecutionContext(new ExecutionContext());
//		stepExecution.setExitStatus(new ExitStatus(false, ExitStatusExceptionClassifier.FATAL_EXCEPTION,
//				"java.lang.Exception"));
//		stepExecutionDao.updateStepExecution(stepExecution);
//		List executions = stepExecutionDao.findStepExecutions(step1, null);
//		assertEquals(1, executions.size());
//		StepExecution tempExecution = (StepExecution) executions.get(0);
//		assertEquals(stepExecution, tempExecution);
//		assertEquals(stepExecution.getExitStatus(), tempExecution.getExitStatus());
//	}

	public void testUpdateStepExecutionWithNullId() {
		StepExecution stepExecution = new StepExecution(null, null, null);
		try {
			stepExecutionDao.updateStepExecution(stepExecution);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testGetStepExecutionCountForNoExecutions() {

//		int executionCount = stepExecutionDao.getStepExecutionCount(step2);
//		assertEquals(executionCount, 0);
	}

	public void testIncrementStepExecutionCount() {

////		assertEquals(1, stepExecutionDao.getStepExecutionCount(step1));
//		StepExecution execution = new StepExecution(step1, new JobExecution(step1.getJobInstance(), new Long(123)),
//				null);
//		stepExecutionDao.saveStepExecution(execution);
////		assertEquals(2, stepExecutionDao.getStepExecutionCount(step1));
	}

	public void testUpdateStepExecutionVersion() throws Exception {
		int before = stepExecution.getVersion().intValue();
		stepExecutionDao.updateStepExecution(stepExecution);
		int after = stepExecution.getVersion().intValue();
		assertEquals("StepExecution version not updated", before + 1, after);
	}

	public void testUpdateStepExecutionOptimisticLocking() throws Exception {
		stepExecution.incrementVersion(); // not really allowed outside dao
		// code
		try {
			stepExecutionDao.updateStepExecution(stepExecution);
			fail("Expected OptimisticLockingFailureException");
		}
		catch (OptimisticLockingFailureException e) {
			// expected
			assertTrue("Exception message should contain step execution id: " + e.getMessage(), e.getMessage().indexOf(
					"" + stepExecution.getId()) >= 0);
			assertTrue("Exception message should contain step execution version: " + e.getMessage(), e.getMessage()
					.indexOf("" + stepExecution.getVersion()) >= 0);
		}
	}
	
	public void testSaveExecutionContext(){
		stepExecution.setExecutionContext(executionContext);
		stepExecutionDao.saveExecutionContext(stepExecution);
		ExecutionContext attributes = stepExecutionDao.findExecutionContext(stepExecution);
		assertEquals(executionContext, attributes);
		executionContext.putString("newString", "newString");
		stepExecutionDao.updateExecutionContext(stepExecution);
		attributes = stepExecutionDao.findExecutionContext(stepExecution);
		assertEquals(executionContext, attributes);
	}
	
	public void testGetLastStepExecution() {
		StepExecution lastExecution = new StepExecution(step1, jobExecution, null);
		lastExecution.setStatus(BatchStatus.STARTED);
		
		int JUMP_INTO_FUTURE = 1000; // makes sure start time is 'greatest'
		lastExecution.setStartTime(new Date(System.currentTimeMillis() + JUMP_INTO_FUTURE));
		stepExecutionDao.saveStepExecution(lastExecution);
		
//		assertEquals(lastExecution, stepExecutionDao.getLastStepExecution(step1, jobExecution));
	}
		
}
