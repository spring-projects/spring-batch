package org.springframework.batch.core.repository.dao;

import static org.junit.Assert.assertNull;

import java.util.Date;

import org.junit.Test;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.SimpleJobRepository;

@RunWith(JUnit4ClassRunner.class)
public class MapStepExecutionDaoTests extends AbstractStepExecutionDaoTests {

	protected StepExecutionDao getStepExecutionDao() {
		return new MapStepExecutionDao();
	}

	protected JobRepository getJobRepository() {
		return new SimpleJobRepository(new MapJobInstanceDao(), new MapJobExecutionDao(), new MapStepExecutionDao(),
				new MapExecutionContextDao());
	}
	
	/**
	 * Modifications to saved entity do not affect the persisted object.
	 */
	@Test
	public void testPersistentCopy() {
		StepExecutionDao tested = new MapStepExecutionDao();
		JobExecution jobExecution = new JobExecution(77L);
		StepExecution stepExecution = new StepExecution("stepName", jobExecution);
		
		assertNull(stepExecution.getEndTime());
		tested.saveStepExecution(stepExecution);
		stepExecution.setEndTime(new Date());
		
		StepExecution retrieved = tested.getStepExecution(jobExecution, stepExecution.getId());
		assertNull(retrieved.getEndTime());
		
		stepExecution.setEndTime(null);
		tested.updateStepExecution(stepExecution);
		stepExecution.setEndTime(new Date());
		
		StepExecution stored = tested.getStepExecution(jobExecution, stepExecution.getId());
		assertNull(stored.getEndTime());
	}

}
