package org.springframework.batch.core.scope.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.scope.context.JobContext;
import org.springframework.batch.core.scope.context.JobSynchronizationManager;

public class JobContextFactoryTests {

	private JobContextFactory factory = new JobContextFactory();

	@After
	public void cleanUp() {
		JobSynchronizationManager.close();
		JobSynchronizationManager.close();
	}

	@Test
	public void testGetContext() {
		JobExecution jobExecution = new JobExecution(11L);
		JobContext context = JobSynchronizationManager.register(jobExecution);
		assertEquals(context, factory.getContext());
	}

	@Test
	public void testGetContextId() {
		JobSynchronizationManager.register(new JobExecution(11L));
		Object id1 = factory.getContextId();
		JobSynchronizationManager.register(new JobExecution(12L));
		Object id2 = factory.getContextId();
		assertFalse(id2.equals(id1));
	}

}
