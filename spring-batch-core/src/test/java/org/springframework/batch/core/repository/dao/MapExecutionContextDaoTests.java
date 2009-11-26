package org.springframework.batch.core.repository.dao;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

/**
 * Tests for {@link MapExecutionContextDao}.
 */
@RunWith(JUnit4ClassRunner.class)
public class MapExecutionContextDaoTests extends AbstractExecutionContextDaoTests {

	@Override
	protected JobInstanceDao getJobInstanceDao() {
		return new MapJobInstanceDao();
	}

	@Override
	protected JobExecutionDao getJobExecutionDao() {
		return new MapJobExecutionDao();
	}

	@Override
	protected StepExecutionDao getStepExecutionDao() {
		return new MapStepExecutionDao();
	}

	@Override
	protected ExecutionContextDao getExecutionContextDao() {
		return new MapExecutionContextDao();
	}
	
	@Test
	public void testPersistentCopy() throws Exception {
		MapExecutionContextDao tested = new MapExecutionContextDao();
		JobExecution jobExecution = new JobExecution((long)1);
		StepExecution stepExecution = new StepExecution("stepName", jobExecution);
		assertTrue(stepExecution.getExecutionContext().isEmpty());
		
		tested.updateExecutionContext(stepExecution);
		stepExecution.getExecutionContext().put("key","value");
		
		ExecutionContext retrieved = tested.getExecutionContext(stepExecution);
		assertTrue(retrieved.isEmpty());
		
		tested.updateExecutionContext(jobExecution);
		jobExecution.getExecutionContext().put("key", "value");
		retrieved = tested.getExecutionContext(jobExecution);
		assertTrue(retrieved.isEmpty());
	}

}

