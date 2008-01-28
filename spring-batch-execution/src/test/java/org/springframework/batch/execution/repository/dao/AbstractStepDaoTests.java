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
import java.util.Properties;

import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.core.executor.ExitCodeExceptionClassifier;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.restart.GenericRestartData;
import org.springframework.batch.restart.RestartData;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;
import org.springframework.util.ClassUtils;

/**
 * Test for StepDao. Because it is very reasonable to assume that there is a
 * foreign key constraint on the JobId of a step, the JobDao is used to create
 * jobs, to have an id for creating steps.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 */
public abstract class AbstractStepDaoTests extends AbstractTransactionalDataSourceSpringContextTests {

	protected JobDao jobDao;

	protected StepDao stepDao;

	protected JobInstance jobInstance;
	
	protected StepInstance step1;
	
	protected StepInstance step2;
	
	protected StepExecution stepExecution;

	protected JobExecution jobExecution;
	
	protected JobParameters jobParameters = new JobParameters();

	public void setJobDao(JobDao jobDao) {
		this.jobDao = jobDao;
	}

	public void setStepDao(StepDao stepDao) {
		this.stepDao = stepDao;
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
		Job job = new Job("TestJob");
		jobInstance = jobDao.createJobInstance(job.getName(), jobParameters);
		step1 = stepDao.createStep(jobInstance, "TestStep1");
		step2 = stepDao.createStep(jobInstance, "TestStep2");
		jobExecution = new JobExecution(step2.getJobInstance());
		
		stepExecution = new StepExecution(step1, jobExecution, null);
		stepExecution.setStatus(BatchStatus.STARTED);
		stepExecution.setStartTime(new Date(System.currentTimeMillis()));
		stepDao.save(stepExecution);
	}
	
	public void testVersionIsNotNullForStep() throws Exception {
		int version = jdbcTemplate.queryForInt("select version from BATCH_STEP_INSTANCE where ID="+step1.getId());
		assertEquals(0, version);
	}
	
	public void testVersionIsNotNullForStepExecution() throws Exception {
		int version = jdbcTemplate.queryForInt("select version from BATCH_STEP_EXECUTION where ID="+stepExecution.getId());
		assertEquals(0, version);
	}
	
	public void testFindStepNull(){
		
		StepInstance step = stepDao.findStep(jobInstance, "UnSavedStep");
		assertNull(step);
	}
	
	public void testFindStep(){
		
		StepInstance tempStep = stepDao.findStep(jobInstance, "TestStep1");
		assertEquals(tempStep, step1);
	}
	
	public void testFindSteps(){
		
		List steps = stepDao.findSteps(jobInstance);
		assertEquals(steps.size(), 2);
		assertTrue(steps.contains(step1));
		assertTrue(steps.contains(step2));
	}
	
	public void testFindStepsNotSaved(){
		
		//no steps are saved for given id, empty list should be returned
		List steps = stepDao.findSteps(new JobInstance(new Long(38922), jobParameters));
		assertEquals(steps.size(), 0);
	}
	
	public void testCreateStep(){
		
		StepInstance step3 = stepDao.createStep(jobInstance, "TestStep3");
		StepInstance tempStep = stepDao.findStep(jobInstance, "TestStep3");
		assertEquals(step3, tempStep);
	}
	
	public void testUpdateStepWithoutRestartData(){
		
		step1.setStatus(BatchStatus.COMPLETED);
		stepDao.update(step1);
		StepInstance tempStep = stepDao.findStep(jobInstance, step1.getName());
		assertEquals(tempStep, step1);
	}
	
	public void testUpdateStepWithRestartData(){
		
		step1.setStatus(BatchStatus.COMPLETED);
		Properties data = new Properties();
		data.setProperty("restart.key1", "restartData");
		RestartData restartData = new GenericRestartData(data);
		step1.setRestartData(restartData);
		stepDao.update(step1);
		StepInstance tempStep = stepDao.findStep(jobInstance, step1.getName());
		assertEquals(tempStep, step1);
		assertEquals(tempStep.getRestartData().getProperties().toString(), 
				restartData.getProperties().toString());
	}
	
	public void testSaveStepExecution(){
		
		StepExecution execution = new StepExecution(step2, jobExecution, null);
		execution.setStatus(BatchStatus.STARTED);
		execution.setStartTime(new Date(System.currentTimeMillis()));
		Properties statistics = new Properties();
		statistics.setProperty("statistic.key1", "0");
		statistics.setProperty("statistic.key2", "5");
		execution.setStatistics(statistics);
		execution.setExitStatus(new ExitStatus(false, ExitCodeExceptionClassifier.FATAL_EXCEPTION, "java.lang.Exception"));
		stepDao.save(execution);
		List executions = stepDao.findStepExecutions(step2);
		assertEquals(1, executions.size());
		StepExecution tempExecution = (StepExecution)executions.get(0);
		assertEquals(execution, tempExecution);
		assertEquals(execution.getStatistics(), tempExecution.getStatistics());
		assertEquals(execution.getExitStatus(), tempExecution.getExitStatus());
	}
	
	public void testUpdateStepExecution(){
		
		stepExecution.setStatus(BatchStatus.COMPLETED);
		stepExecution.setEndTime(new Date(System.currentTimeMillis()));
		stepExecution.setCommitCount(5);
		stepExecution.setTaskCount(5);
		stepExecution.setStatistics(new Properties());
		stepExecution.setExitStatus(new ExitStatus(false, ExitCodeExceptionClassifier.FATAL_EXCEPTION, "java.lang.Exception"));
		stepDao.update(stepExecution);
		List executions = stepDao.findStepExecutions(step1);
		assertEquals(1, executions.size());
		StepExecution tempExecution = (StepExecution)executions.get(0);
		assertEquals(stepExecution, tempExecution);
		assertEquals(stepExecution.getExitStatus(), tempExecution.getExitStatus());
	}
	
	public void testUpdateStepExecutionWithNullId(){
		StepExecution stepExecution = new StepExecution(null, null, null);
		try{
			stepDao.update(stepExecution);
			fail("Expected IllegalArgumentException");
		}catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	public void testGetStepExecutionCountForNoExecutions(){
		
		int executionCount = stepDao.getStepExecutionCount(step2);
		assertEquals(executionCount, 0);
	}

	public void testIncrementStepExecutionCount(){
		
		assertEquals(1, stepDao.getStepExecutionCount(step1));
		StepExecution execution = new StepExecution(step1, new JobExecution(step1.getJobInstance(), new Long(123)), null);
		stepDao.save(execution);
		assertEquals(2, stepDao.getStepExecutionCount(step1));
	}
		
	public void testUpdateStepExecutionVersion() throws Exception {
		int before = stepExecution.getVersion().intValue();
		stepDao.update(stepExecution);
		int after = stepExecution.getVersion().intValue();
		assertEquals("StepExecution version not updated", before+1, after);
	}

	public void testUpdateStepExecutionOptimisticLocking() throws Exception {
		stepExecution.incrementVersion(); // not really allowed outside dao code
		try {
			stepDao.update(stepExecution);
			fail("Expected OptimisticLockingFailureException");
		}
		catch (OptimisticLockingFailureException e) {
			// expected
			assertTrue("Exception message should contain step execution id: "+e.getMessage(), e.getMessage().indexOf(""+stepExecution.getId())>=0);
			assertTrue("Exception message should contain step execution version: "+e.getMessage(), e.getMessage().indexOf(""+stepExecution.getVersion())>=0);
		}
	}
	
}
