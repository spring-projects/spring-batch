/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.scope.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.jsr.configuration.support.BatchPropertyContext;

public class StepSynchronizationManagerTests {

	private StepExecution stepExecution = new StepExecution("step", new JobExecution(0L));
	private BatchPropertyContext propertyContext = new BatchPropertyContext();

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
	public void testGetContextWithBatchProperties() {
		StepContext context = StepSynchronizationManager.getContext();
		assertNull(context);
		StepSynchronizationManager.register(stepExecution, propertyContext);
		context = StepSynchronizationManager.getContext();
		assertNotNull(context);
		assertEquals(stepExecution, context.getStepExecution());
	}

	@Test
	public void testClose() throws Exception {
		final List<String> list = new ArrayList<>();
		StepContext context = StepSynchronizationManager.register(stepExecution);
		context.registerDestructionCallback("foo", new Runnable() {
			@Override
			public void run() {
				list.add("foo");
			}
		});
		StepSynchronizationManager.close();
		assertNull(StepSynchronizationManager.getContext());
		assertEquals(0, list.size());
	}

	@Test
	public void testMultithreaded() throws Exception {
		StepContext context = StepSynchronizationManager.register(stepExecution);
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		FutureTask<StepContext> task = new FutureTask<>(new Callable<StepContext>() {
			@Override
			public StepContext call() throws Exception {
				try {
					StepSynchronizationManager.register(stepExecution);
					StepContext context = StepSynchronizationManager.getContext();
					context.setAttribute("foo", "bar");
					return context;
				}
				finally {
					StepSynchronizationManager.close();
				}
			}
		});
		executorService.execute(task);
		executorService.awaitTermination(1, TimeUnit.SECONDS);
		assertEquals(context.attributeNames().length, task.get().attributeNames().length);
		StepSynchronizationManager.close();
		assertNull(StepSynchronizationManager.getContext());
	}

	@Test
	public void testRelease() {
		StepContext context = StepSynchronizationManager.register(stepExecution);
		final List<String> list = new ArrayList<>();
		context.registerDestructionCallback("foo", new Runnable() {
			@Override
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
