package org.springframework.batch.core.repository.dao.gemfire;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"batch-gemfire-context.xml","cache-context.xml"})
public class GemfireJobExecutionDaoTest {

	@Resource
	private GemfireJobExecutionDao jobExecutionDao;
	
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
	}
	
	
	@Test
	public void testSaveJobExecution() {
		//retrieve the instance
		JobInstance instance = jobInstanceDao.getJobInstance(jobInstanceId);
		JobExecution jobExecution = new JobExecution(instance,parameters);
		
		jobExecutionDao.saveJobExecution(jobExecution);
		//check the jobExecution for an id
		assertNotNull(jobExecution.getJobId());
		assertNotNull(jobExecution.getId());
	}

	@Test
	public void testUpdateJobExecution() {
		//retrieve the instance
		JobInstance instance = jobInstanceDao.getJobInstance(jobInstanceId);
		JobExecution jobExecution = new JobExecution(instance,parameters);
		
		jobExecutionDao.saveJobExecution(jobExecution);
		//check the jobExecution for an id
		assertNotNull(jobExecution.getJobId());
		assertNotNull(jobExecution.getId());
		//get the updated date
		//update it
		jobExecution.upgradeStatus(BatchStatus.COMPLETED);
		//save
		jobExecutionDao.updateJobExecution(jobExecution);
		//lets retrieve and check the status
		JobExecution execution = jobExecutionDao.getJobExecution(jobExecution.getId());
		assertEquals(execution.getStatus(),BatchStatus.COMPLETED);
		
	}

	@Test
	public void testFindJobExecutions() {
		//retrieve the instance
		JobInstance instance = jobInstanceDao.getJobInstance(jobInstanceId);		
		List<JobExecution> executions = jobExecutionDao.findJobExecutions(instance);
		assertNotNull(executions);
		assertTrue(executions.isEmpty());
	}

	@Test
	public void testGetLastJobExecution() throws Exception {
		//retrieve the instance
		JobInstance instance = jobInstanceDao.getJobInstance(jobInstanceId);
		//loop and 'add' a few job executions
		for (int i=0;i<5;i++) {
			JobExecution jobExecution = new JobExecution(instance,parameters);
			jobExecutionDao.saveJobExecution(jobExecution);
			//pause
			Thread.sleep(10);
		}//end if
		//retrieve
		JobExecution execution = jobExecutionDao.getLastJobExecution(instance);
		assertNotNull(execution);
	}
	
	@Test
	public void testGetLastJobExecutionNull() throws Exception {
		//retrieve the instance
		JobInstance instance = jobInstanceDao.getJobInstance(jobInstanceId);
		//retrieve
		JobExecution execution = jobExecutionDao.getLastJobExecution(instance);
		assertNull(execution);//expect null - no executions for this jobinstance
	}	

	@Test
	public void testFindRunningJobExecutions() {
		//retrieve the instance
		JobInstance instance = jobInstanceDao.getJobInstance(jobInstanceId);		
		//execute
		Set<JobExecution> executions = jobExecutionDao.findRunningJobExecutions(instance.getJobName());
		assertNotNull(executions);
		assertTrue(executions.isEmpty());
	}

	@Test
	public void testGetJobExecution() {
		//create a job execution
		JobInstance instance = jobInstanceDao.getJobInstance(jobInstanceId);
		JobExecution jobExecution = new JobExecution(instance,parameters);
		
		jobExecutionDao.saveJobExecution(jobExecution);
		
		Long executionId = jobExecution.getId();
		//now retrieve it
		JobExecution retrieved = jobExecutionDao.getJobExecution(executionId);
		assertTrue(retrieved.getCreateTime().equals(jobExecution.getCreateTime()));
	}

	@Test
	public void testSynchronizeStatus() {
		//save a job execution
		JobInstance instance = jobInstanceDao.getJobInstance(jobInstanceId);
		JobExecution jobExecution = new JobExecution(instance,parameters);
		
		jobExecutionDao.saveJobExecution(jobExecution);
		
		BatchStatus originalStatus = jobExecution.getStatus();
		
		assertTrue(jobExecution.getVersion() == 0);
		//locally, update it's version (version + 1)
		jobExecution.setVersion(jobExecution.getVersion() + 1);
		//update it's status (e.g. ABANDONED)
		jobExecution.setStatus(BatchStatus.ABANDONED);
		//synchronize
		jobExecutionDao.synchronizeStatus(jobExecution);
		//verify the affect
		JobExecution result = jobExecutionDao.getJobExecution(jobExecution.getId());
		assertTrue(result.getVersion() == 0);//reverted
		assertTrue(result.getStatus() == originalStatus);
	}

}
