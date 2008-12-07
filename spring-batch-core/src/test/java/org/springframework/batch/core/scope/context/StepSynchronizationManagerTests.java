package org.springframework.batch.core.scope.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;

public class StepSynchronizationManagerTests {

	private StepExecution stepExecution = new StepExecution("step", new JobExecution(0L));

	@Before
	@After
	public void start() {
		while (StepSynchronizationManager.getContext() != null) {
			StepSynchronizationManager.close();
		}
	}

	@Test
	public void testGetContext() {
		assertNull(StepSynchronizationManager.getContext());
		StepSynchronizationManager.register(stepExecution);
		assertNotNull(StepSynchronizationManager.getContext());
	}

	@Test
	public void testClose() {
		final List<String> list = new ArrayList<String>();
		StepContext context = StepSynchronizationManager.register(stepExecution);
		context.registerDestructionCallback("foo", new Runnable() {
			public void run() {
				list.add("foo");
			}
		});
		StepSynchronizationManager.close();
		assertNull(StepSynchronizationManager.getContext());
		assertEquals(0, list.size());
	}

	@Test
	public void testRelease() {
		StepContext context = StepSynchronizationManager.register(stepExecution);
		final List<String> list = new ArrayList<String>();
		context.registerDestructionCallback("foo", new Runnable() {
			public void run() {
				list.add("foo");
			}
		});
		// On release we expect the destruction callbacks to be called
		StepSynchronizationManager.release();
		assertNull(StepSynchronizationManager.getContext());
		assertEquals(1, list.size());
	}

	@Test
	public void testRegisterNull() {
		assertNull(StepSynchronizationManager.getContext());
		StepSynchronizationManager.register(null);
		assertNull(StepSynchronizationManager.getContext());
	}

	@Test
	public void testRegisterTwice() {
		StepSynchronizationManager.register(stepExecution);
		StepSynchronizationManager.register(stepExecution);
		StepSynchronizationManager.close();
		// if someone registers you have to assume they are going to close, so
		// the last thing you want is for the close to remove another context
		// that someone else has registered
		assertNotNull(StepSynchronizationManager.getContext());
		StepSynchronizationManager.close();
		assertNull(StepSynchronizationManager.getContext());
	}

}
