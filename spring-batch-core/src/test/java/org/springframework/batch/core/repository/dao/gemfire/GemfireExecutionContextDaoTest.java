package org.springframework.batch.core.repository.dao.gemfire;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"batch-gemfire-context.xml","cache-context.xml"})
public class GemfireExecutionContextDaoTest {

	@Resource
	private ExecutionContextDao executionContextDao;

	@Resource
	private GemfireJobExecutionDao jobExecutionDao;
	
	@Resource
	private GemfireStepExecutionDao stepExecutionDao;	
	
	@Resource
	private GemfireJobInstanceDao jobInstanceDao;
	
	private Long jobInstanceId;
	
	private JobParameters parameters;
	
	@Before
	public void before() throws Exception {
		parameters = new JobParametersBuilder().addLong("runtime",System.currentTimeMillis()).toJobParameters();
		//presist
		JobInstance jobInstance = jobInstanceDao.createJobInstance("a job", parameters);
		//set
		jobInstanceId = jobInstance.getId();
		System.out.println("job instance id: " + jobInstanceId);
	}
	
	@After
	public void after() throws Exception {
		jobInstanceDao.delete(jobInstanceId);
		jobExecutionDao.deleteAll();
		stepExecutionDao.deleteAll();
	}	
	
	@Test
	public void testGetExecutionContextJobExecution() {
		//retrieve the instance
		JobInstance instance = jobInstanceDao.getJobInstance(jobInstanceId);
		JobExecution jobExecution = new JobExecution(instance,parameters);
		
		jobExecutionDao.saveJobExecution(jobExecution);
		//check the existing executionContext
		assertNotNull(jobExecution.getExecutionContext());
		assertTrue(jobExecution.getExecutionContext().isEmpty());
		//now retrieve via the execution context and see
		ExecutionContext context = executionContextDao.getExecutionContext(jobExecution);
		assertNotNull(context);
		assertTrue(context.isEmpty());
	}

	@Test
	public void testGetExecutionContextStepExecution() {
		//retrieve the instance
		JobInstance instance = jobInstanceDao.getJobInstance(jobInstanceId);
		JobExecution jobExecution = new JobExecution(instance,parameters);
		
		jobExecutionDao.saveJobExecution(jobExecution);
		StepExecution stepExecution = new StepExecution("step name", jobExecution);
		stepExecutionDao.saveStepExecution(stepExecution);
		//check the existing execution Context
		assertNotNull(stepExecution.getExecutionContext());
		assertTrue(stepExecution.getExecutionContext().isEmpty());
		//now retrieve via the execution context and see
		ExecutionContext context = executionContextDao.getExecutionContext(stepExecution);
		assertNotNull(context);
		assertTrue(context.isEmpty());		
	}

	@Test
	public void testSaveExecutionContextJobExecution() {
		//retrieve the instance
		JobInstance instance = jobInstanceDao.getJobInstance(jobInstanceId);
		JobExecution jobExecution = new JobExecution(instance,parameters);
		
		jobExecutionDao.saveJobExecution(jobExecution);
		//check the existing execution Context
		assertNotNull(jobExecution.getExecutionContext());
		assertTrue(jobExecution.getExecutionContext().isEmpty());
		//add something
		jobExecution.getExecutionContext().putString("a key", "a value");
		//now retrieve via the execution context and see
		executionContextDao.saveExecutionContext(jobExecution);
		//retrieve the step execution from gemfire by id
		JobExecution execution = jobExecutionDao.findOne(jobExecution.getId());
		//get the context
		ExecutionContext context = executionContextDao.getExecutionContext(execution);
		//check
		assertTrue(context.containsKey("a key"));
	}

	@Test
	public void testSaveExecutionContextStepExecution() {
		//retrieve the instance
		JobInstance instance = jobInstanceDao.getJobInstance(jobInstanceId);
		JobExecution jobExecution = new JobExecution(instance,parameters);
		
		jobExecutionDao.saveJobExecution(jobExecution);
		StepExecution stepExecution = new StepExecution("step name", jobExecution);
		stepExecutionDao.saveStepExecution(stepExecution);
		//check the existing execution Context
		assertNotNull(stepExecution.getExecutionContext());
		assertTrue(stepExecution.getExecutionContext().isEmpty());
		//add something
		stepExecution.getExecutionContext().putString("a key", "a value");
		//now retrieve via the execution context and see
		executionContextDao.saveExecutionContext(stepExecution);
		//retrieve the step execution from gemfire by id
		StepExecution execution = stepExecutionDao.findOne(stepExecution.getId());
		//get the context
		ExecutionContext context = executionContextDao.getExecutionContext(execution);
		//check
		assertTrue(context.containsKey("a key"));		
	}

}
