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

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.batch.core.step.ExitStatusExceptionClassifier;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.ExitStatus;
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

	protected Step step1;

	protected Step step2;

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
		jobInstance = jobInstanceDao.createJobInstance(job, jobParameters);
		step1 = new StepSupport("TestStep1");
		step2 = new StepSupport("TestStep2");
		jobExecution = new JobExecution(jobInstance);
		jobExecutionDao.saveJobExecution(jobExecution);

		stepExecution = new StepExecution(step1, jobExecution, new Long(1));
		stepExecution.setStatus(BatchStatus.STARTED);
		stepExecution.setStartTime(new Date(System.currentTimeMillis()));
		stepExecutionDao.saveStepExecution(stepExecution);
		
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
		stepExecutionDao.saveOrUpdateExecutionContext(stepExecution);
		ExecutionContext tempAttributes = stepExecutionDao.findExecutionContext(stepExecution);
		assertEquals(executionContext, tempAttributes);
	}
	
	public void testSaveStepExecution() {
		StepExecution execution = new StepExecution(step2, jobExecution, null);
		execution.setStatus(BatchStatus.STARTED);
		execution.setStartTime(new Date(System.currentTimeMillis()));
		execution.setExitStatus(new ExitStatus(false, ExitStatusExceptionClassifier.FATAL_EXCEPTION,
				"java.lang.Exception"));
		stepExecutionDao.saveStepExecution(execution);
		StepExecution retrievedExecution = stepExecutionDao.getStepExecution(jobExecution, step2);
		assertNotNull(retrievedExecution);
		assertEquals(execution, retrievedExecution);
		assertEquals(execution.getExitStatus(), retrievedExecution.getExitStatus());
	}

	public void testSaveStepExecutionAndExecutionContext() {
		StepExecution execution = new StepExecution(step2, jobExecution, null);
		execution.setStatus(BatchStatus.STARTED);
		execution.setStartTime(new Date(System.currentTimeMillis()));
		execution.setExecutionContext(executionContext);
		execution.setExitStatus(new ExitStatus(false, ExitStatusExceptionClassifier.FATAL_EXCEPTION,
				"java.lang.Exception"));
		stepExecutionDao.saveStepExecution(execution);
		stepExecutionDao.saveOrUpdateExecutionContext(execution);
		StepExecution retrievedExecution = stepExecutionDao.getStepExecution(jobExecution, step2);
		assertNotNull(retrievedExecution);
		assertEquals(execution, retrievedExecution);
		assertEquals(execution.getExecutionContext().getString("1"), retrievedExecution.getExecutionContext().getString("1"));
		assertEquals(execution.getExecutionContext().getLong("3"), retrievedExecution.getExecutionContext().getLong("3"));
		assertEquals(execution.getExitStatus(), retrievedExecution.getExitStatus());
	}

	public void testUpdateStepExecution() {

		stepExecution.setStatus(BatchStatus.COMPLETED);
		stepExecution.setEndTime(new Date(System.currentTimeMillis()));
		stepExecution.setCommitCount(5);
		stepExecution.setItemCount(5);
		stepExecution.setExecutionContext(new ExecutionContext());
		stepExecution.setExitStatus(new ExitStatus(false, ExitStatusExceptionClassifier.FATAL_EXCEPTION,
				"java.lang.Exception"));
		stepExecutionDao.updateStepExecution(stepExecution);
		StepExecution retrievedExecution = stepExecutionDao.getStepExecution(jobExecution, step1);
		assertNotNull(retrievedExecution);
		assertEquals(stepExecution, retrievedExecution);
		assertEquals(stepExecution.getExitStatus(), retrievedExecution.getExitStatus());
	}

	public void testUpdateStepExecutionWithNullId() {
		StepExecution stepExecution = new StepExecution(new StepSupport("testStep"), null, null);
		try {
			stepExecutionDao.updateStepExecution(stepExecution);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
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
		stepExecutionDao.saveOrUpdateExecutionContext(stepExecution);
		ExecutionContext attributes = stepExecutionDao.findExecutionContext(stepExecution);
		assertEquals(executionContext, attributes);
		executionContext.putString("newString", "newString");
		executionContext.putLong("newLong", 1);
		executionContext.putDouble("newDouble", 2.5);
		executionContext.put("newSerializable", "serializableValue");
		stepExecutionDao.saveOrUpdateExecutionContext(stepExecution);
		attributes = stepExecutionDao.findExecutionContext(stepExecution);
		assertEquals(executionContext, attributes);
	}
	
	public void testGetStepExecution() {
		assertEquals(stepExecution, stepExecutionDao.getStepExecution(jobExecution, step1));
	}
		
}
