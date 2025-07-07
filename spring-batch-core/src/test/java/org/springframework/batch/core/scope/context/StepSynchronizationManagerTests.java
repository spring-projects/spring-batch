/*
 * Copyright 2013-2023 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;

class StepSynchronizationManagerTests {

	private final StepExecution stepExecution = new StepExecution("step", new JobExecution(0L));

	@BeforeEach
	@AfterEach
	void start() {
		while (StepSynchronizationManager.getContext() != null) {
			StepSynchronizationManager.close();
		}
	}

	@Test
	void testGetContext() {
		assertNull(StepSynchronizationManager.getContext());
		StepSynchronizationManager.register(stepExecution);
		assertNotNull(StepSynchronizationManager.getContext());
	}

	@Test
	void testClose() {
		final List<String> list = new ArrayList<>();
		StepContext context = StepSynchronizationManager.register(stepExecution);
		context.registerDestructionCallback("foo", () -> list.add("foo"));
		StepSynchronizationManager.close();
		assertNull(StepSynchronizationManager.getContext());
		assertEquals(0, list.size());
	}

	@Test
	void testMultithreaded() throws Exception {
		StepContext context = StepSynchronizationManager.register(stepExecution);
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		FutureTask<StepContext> task = new FutureTask<>(() -> {
			try {
				StepSynchronizationManager.register(stepExecution);
				StepContext context1 = StepSynchronizationManager.getContext();
				context1.setAttribute("foo", "bar");
				return context1;
			}
			finally {
				StepSynchronizationManager.close();
			}
		});
		executorService.execute(task);
		executorService.awaitTermination(1, TimeUnit.SECONDS);
		assertEquals(context.attributeNames().length, task.get().attributeNames().length);
		StepSynchronizationManager.close();
		assertNull(StepSynchronizationManager.getContext());
	}

	@Test
	void testRelease() {
		StepContext context = StepSynchronizationManager.register(stepExecution);
		final List<String> list = new ArrayList<>();
		context.registerDestructionCallback("foo", () -> list.add("foo"));
		// On release we expect the destruction callbacks to be called
		StepSynchronizationManager.release();
		assertNull(StepSynchronizationManager.getContext());
		assertEquals(1, list.size());
	}

	@Test
	void testRegisterNull() {
		assertNull(StepSynchronizationManager.getContext());
		StepSynchronizationManager.register(null);
		assertNull(StepSynchronizationManager.getContext());
	}

	@Test
	void testRegisterTwice() {
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
