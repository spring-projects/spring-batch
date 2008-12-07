package org.springframework.batch.core.scope.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;

public class StepContextFactoryTests {

	private StepContextFactory factory = new StepContextFactory();

	@After
	public void cleanUp() {
		StepSynchronizationManager.close();
		StepSynchronizationManager.close();
	}

	@Test
	public void testGetContext() {
		StepExecution stepExecution = new StepExecution("foo", new JobExecution(11L));
		StepContext context = StepSynchronizationManager.register(stepExecution);
		assertEquals(context, factory.getContext());
	}

	@Test
	public void testGetContextId() {
		StepSynchronizationManager.register(new StepExecution("foo", new JobExecution(11L), 0L));
		Object id1 = factory.getContextId();
		StepSynchronizationManager.register(new StepExecution("foo", new JobExecution(12L), 1L));
		Object id2 = factory.getContextId();
		assertFalse(id2.equals(id1));
	}

}
