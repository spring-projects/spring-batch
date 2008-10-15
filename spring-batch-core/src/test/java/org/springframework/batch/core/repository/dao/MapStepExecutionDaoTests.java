package org.springframework.batch.core.repository.dao;

import java.util.Date;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.SimpleJobRepository;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ExecutionContext;

public class MapStepExecutionDaoTests extends AbstractStepExecutionDaoTests {

	protected StepExecutionDao getStepExecutionDao() {
		return new MapStepExecutionDao();
	}

	protected JobRepository getJobRepository() {
		MapJobInstanceDao.clear();
		MapJobExecutionDao.clear();
		MapStepExecutionDao.clear();
		return new SimpleJobRepository(new MapJobInstanceDao(), new MapJobExecutionDao(), new MapStepExecutionDao());
	}
	
	/**
	 * Modifications to saved entity do not affect the persisted object.
	 */
	public void testPersistentCopy() {
		StepExecutionDao tested = new MapStepExecutionDao();
		TaskletStep step = new TaskletStep();
		step.setName("stepName");
		
		JobExecution jobExecution = new JobExecution(new JobInstance(new Long(1), new JobParameters(), "jobName"), new Long(1));
		StepExecution stepExecution = new StepExecution("stepName", jobExecution);
		
		assertNull(stepExecution.getEndTime());
		tested.saveStepExecution(stepExecution);
		stepExecution.setEndTime(new Date());
		
		StepExecution retrieved = tested.getStepExecution(jobExecution, step);
		assertNull(retrieved.getEndTime());
		
		stepExecution.setEndTime(null);
		tested.updateStepExecution(stepExecution);
		stepExecution.setEndTime(new Date());
		
		StepExecution stored = tested.getStepExecution(jobExecution, step);
		assertNull(stored.getEndTime());
		
		tested.saveOrUpdateExecutionContext(stepExecution);
		stepExecution.getExecutionContext().put("key", "value");
		ExecutionContext saved = tested.findExecutionContext(stepExecution);
		assertTrue(saved.isEmpty());
	}

}
