/*
 * Copyright 2013-2022 the original author or authors.
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;

/**
 * JobSynchronizationManagerTests.
 *
 * @author Jimmy Praet
 */
class JobSynchronizationManagerTests {

	private final JobExecution jobExecution = new JobExecution(0L);

	@BeforeEach
	@AfterEach
	void start() {
		while (JobSynchronizationManager.getContext() != null) {
			JobSynchronizationManager.close();
		}
	}

	@Test
	void testGetContext() {
		assertNull(JobSynchronizationManager.getContext());
		JobSynchronizationManager.register(jobExecution);
		assertNotNull(JobSynchronizationManager.getContext());
	}

	@Test
	void testClose() {
		final List<String> list = new ArrayList<>();
		JobContext context = JobSynchronizationManager.register(jobExecution);
		context.registerDestructionCallback("foo", new Runnable() {
			@Override
			public void run() {
				list.add("foo");
			}
		});
		JobSynchronizationManager.close();
		assertNull(JobSynchronizationManager.getContext());
		assertEquals(0, list.size());
	}

	@Test
	void testMultithreaded() throws Exception {
		JobContext context = JobSynchronizationManager.register(jobExecution);
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		FutureTask<JobContext> task = new FutureTask<>(new Callable<JobContext>() {
			@Override
			public JobContext call() throws Exception {
				try {
					JobSynchronizationManager.register(jobExecution);
					JobContext context = JobSynchronizationManager.getContext();
					context.setAttribute("foo", "bar");
					return context;
				}
				finally {
					JobSynchronizationManager.close();
				}
			}
		});
		executorService.execute(task);
		executorService.awaitTermination(1, TimeUnit.SECONDS);
		assertEquals(context.attributeNames().length, task.get().attributeNames().length);
		JobSynchronizationManager.close();
		assertNull(JobSynchronizationManager.getContext());
	}

	@Test
	void testRelease() {
		JobContext context = JobSynchronizationManager.register(jobExecution);
		final List<String> list = new ArrayList<>();
		context.registerDestructionCallback("foo", new Runnable() {
			@Override
			public void run() {
				list.add("foo");
			}
		});
		// On release we expect the destruction callbacks to be called
		JobSynchronizationManager.release();
		assertNull(JobSynchronizationManager.getContext());
		assertEquals(1, list.size());
	}

	@Test
	void testRegisterNull() {
		assertNull(JobSynchronizationManager.getContext());
		JobSynchronizationManager.register(null);
		assertNull(JobSynchronizationManager.getContext());
	}

	@Test
	void testRegisterTwice() {
		JobSynchronizationManager.register(jobExecution);
		JobSynchronizationManager.register(jobExecution);
		JobSynchronizationManager.close();
		// if someone registers you have to assume they are going to close, so
		// the last thing you want is for the close to remove another context
		// that someone else has registered
		assertNotNull(JobSynchronizationManager.getContext());
		JobSynchronizationManager.close();
		assertNull(JobSynchronizationManager.getContext());
	}

}
