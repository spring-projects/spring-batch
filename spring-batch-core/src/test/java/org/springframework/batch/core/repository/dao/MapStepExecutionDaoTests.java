package org.springframework.batch.core.repository.dao;

import static org.junit.Assert.*;

import java.util.Date;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.SimpleJobRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.internal.runners.JUnit4ClassRunner;

@RunWith(JUnit4ClassRunner.class)
public class MapStepExecutionDaoTests extends AbstractStepExecutionDaoTests {

	protected StepExecutionDao getStepExecutionDao() {
		return new MapStepExecutionDao();
	}

	protected JobRepository getJobRepository() {
		MapJobInstanceDao.clear();
		MapJobExecutionDao.clear();
		MapStepExecutionDao.clear();
		return new SimpleJobRepository(new MapJobInstanceDao(), new MapJobExecutionDao(), new MapStepExecutionDao(),
				new MapExecutionContextDao());
	}
	
	/**
	 * Modifications to saved entity do not affect the persisted object.
	 */
	@Test
	public void testPersistentCopy() {
		StepExecutionDao tested = new MapStepExecutionDao();
		JobExecution jobExecution = new JobExecution((long) 77);
		StepExecution stepExecution = new StepExecution("stepName", jobExecution);
		
		assertNull(stepExecution.getEndTime());
		tested.saveStepExecution(stepExecution);
		stepExecution.setEndTime(new Date());
		
		StepExecution retrieved = tested.getStepExecution(jobExecution, "stepName");
		assertNull(retrieved.getEndTime());
		
		stepExecution.setEndTime(null);
		tested.updateStepExecution(stepExecution);
		stepExecution.setEndTime(new Date());
		
		StepExecution stored = tested.getStepExecution(jobExecution, "stepName");
		assertNull(stored.getEndTime());
	}

}
