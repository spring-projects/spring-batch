package org.springframework.batch.core.repository.dao;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Ignore;
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
	public void testSaveBothJobAndStepContextWithSameId() throws Exception {
		MapExecutionContextDao tested = new MapExecutionContextDao();
		JobExecution jobExecution = new JobExecution(1L);
		StepExecution stepExecution = new StepExecution("stepName", jobExecution, 1L);
		
		assertTrue(stepExecution.getId() == jobExecution.getId());
		
		jobExecution.getExecutionContext().put("type", "job");
		stepExecution.getExecutionContext().put("type", "step");
		assertTrue(!jobExecution.getExecutionContext().get("type").equals(stepExecution.getExecutionContext().get("type")));
		assertEquals("job", jobExecution.getExecutionContext().get("type"));
		assertEquals("step", stepExecution.getExecutionContext().get("type"));

		tested.saveExecutionContext(jobExecution);
		tested.saveExecutionContext(stepExecution);
	
		ExecutionContext jobCtx = tested.getExecutionContext(jobExecution);
		ExecutionContext stepCtx = tested.getExecutionContext(stepExecution);

		assertEquals("job", jobCtx.get("type"));
		assertEquals("step", stepCtx.get("type"));
	}

	@Test
	public void testPersistentCopy() throws Exception {
		MapExecutionContextDao tested = new MapExecutionContextDao();
		JobExecution jobExecution = new JobExecution((long)1);
		StepExecution stepExecution = new StepExecution("stepName", jobExecution, 123L);
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

