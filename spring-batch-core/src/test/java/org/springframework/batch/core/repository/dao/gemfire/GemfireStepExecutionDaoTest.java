package org.springframework.batch.core.repository.dao.gemfire;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"batch-gemfire-context.xml","cache-context.xml"})
public class GemfireStepExecutionDaoTest {

	@Resource
	private GemfireStepExecutionDao stepExecutionDao;

	@Resource
	private GemfireJobExecutionDao jobExecutionDao;
	
	@Resource
	private GemfireJobInstanceDao jobInstanceDao;
	
	private Long jobInstanceId;
	
	private JobParameters parameters;
	
	private Long jobExecutionId;
	
	@Before
	public void before() throws Exception {
		parameters = new JobParametersBuilder().addLong("runtime",System.currentTimeMillis()).toJobParameters();
		//presist
		JobInstance jobInstance = jobInstanceDao.createJobInstance("a job", parameters);
		//set
		jobInstanceId = jobInstance.getId();
		//build
		JobExecution jobExecution = new JobExecution(jobInstance,parameters);
		jobExecutionDao.saveJobExecution(jobExecution);
		jobExecutionId = jobExecution.getId();
		
	}
	
	@After
	public void after() throws Exception {
		jobInstanceDao.delete(jobInstanceId);
		jobExecutionDao.deleteAll();
	}	
	
	@Test
	public void testSaveStepExecution() {
		JobExecution jobExecution = jobExecutionDao.getJobExecution(jobExecutionId);
		//create a step execution
		StepExecution execution = new StepExecution("some step", jobExecution);
		stepExecutionDao.saveStepExecution(execution);
		//get the id
		assertNotNull(execution.getId());
		
	}

	@Test
	public void testSaveStepExecutions() {
		//check the count
		long count = stepExecutionDao.count();
		JobExecution jobExecution = jobExecutionDao.getJobExecution(jobExecutionId);
		List<StepExecution> executions = new ArrayList<StepExecution>();
		//create a set of steps
		for (int i=0;i<5;i++) {
			StepExecution execution = new StepExecution("some step " + i, jobExecution);
			executions.add(execution);
		}//end for
		//save
		stepExecutionDao.saveStepExecutions(executions);
		//test
		assertTrue((count+5) == stepExecutionDao.count());
	}

	@Test
	public void testUpdateStepExecution() {
		JobExecution jobExecution = jobExecutionDao.getJobExecution(jobExecutionId);
		//create a step execution
		StepExecution execution = new StepExecution("some step", jobExecution);
		//save normally
		stepExecutionDao.saveStepExecution(execution);
		//get the version
		int version = execution.getVersion();
		//update
		stepExecutionDao.updateStepExecution(execution);
		//check the version
		assertTrue((version + 1) == execution.getVersion());
	}

	@Test
	public void testGetStepExecution() {
		JobExecution jobExecution = jobExecutionDao.getJobExecution(jobExecutionId);
		//create a step execution
		StepExecution execution = new StepExecution("some step", jobExecution);
		//save normally
		stepExecutionDao.saveStepExecution(execution);
		//get the id
		Long stepExecutionId = execution.getId();
		
		StepExecution result = stepExecutionDao.getStepExecution(jobExecution, stepExecutionId);
		//test
		assertEquals(result.getStepName(),execution.getStepName());
	}

	@Test
	public void testAddStepExecutions() {
		//setup a bunch of step executions for this job
		JobExecution jobExecution = jobExecutionDao.getJobExecution(jobExecutionId);
		List<StepExecution> executions = new ArrayList<StepExecution>();
		//create a set of steps
		for (int i=0;i<5;i++) {
			StepExecution execution = new StepExecution("some step " + i, jobExecution);
			executions.add(execution);
		}//end for
		//save
		stepExecutionDao.saveStepExecutions(executions);		
		//pull back the job execution from the repo
		JobExecution retrieved = jobExecutionDao.getJobExecution(jobExecutionId);
		//check if there's any 
		assertTrue(retrieved.getStepExecutions().isEmpty());
		
		stepExecutionDao.addStepExecutions(retrieved);
		//retry
		assertFalse(retrieved.getStepExecutions().isEmpty());
	}

}
