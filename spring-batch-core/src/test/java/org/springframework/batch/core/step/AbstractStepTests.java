/*
 * Copyright 2024-2025 the original author or authors.
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
package org.springframework.batch.core.step;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AbstractStep}.
 */
class AbstractStepTests {

	@Test
	void testEndTimeInListener() throws Exception {
		// given
		StepExecution execution = new StepExecution(1L, "step",
				new JobExecution(0L, new JobInstance(1L, "job"), new JobParameters()));
		JobRepository jobRepository = mock();
		AbstractStep tested = new AbstractStep(jobRepository) {
			@Override
			protected void doExecute(StepExecution stepExecution) {
			}
		};
		Listener stepListener = new Listener();
		tested.setStepExecutionListeners(new StepExecutionListener[] { stepListener });
		tested.setJobRepository(jobRepository);

		// when
		tested.execute(execution);

		// then
		assertNotNull(stepListener.getStepEndTime());
	}

	@Test
	void testCallUnderLockSerializesConcurrentUpdates() throws Exception {
		// given
		StepExecution execution = new StepExecution(1L, "step",
				new JobExecution(0L, new JobInstance(1L, "job"), new JobParameters()));
		JobRepository jobRepository = mock();
		AtomicInteger concurrent = new AtomicInteger();
		AtomicBoolean overlap = new AtomicBoolean(false);
		AtomicInteger completed = new AtomicInteger();
		int threads = 8;
		int iterations = 200;
		AbstractStep tested = new AbstractStep(jobRepository) {
			@Override
			protected void doExecute(StepExecution stepExecution) throws Exception {
				// While the step is executing, the per-execution lock is registered, so
				// concurrent callers (the worker and a stopping thread) are serialized.
				ExecutorService pool = Executors.newFixedThreadPool(threads);
				try {
					List<Future<?>> futures = new ArrayList<>();
					for (int t = 0; t < threads; t++) {
						futures.add(pool.submit(() -> {
							for (int i = 0; i < iterations; i++) {
								callUnderLock(stepExecution, () -> {
									if (concurrent.incrementAndGet() != 1) {
										overlap.set(true);
									}
									completed.incrementAndGet();
									concurrent.decrementAndGet();
								});
							}
						}));
					}
					for (Future<?> future : futures) {
						future.get();
					}
				}
				finally {
					pool.shutdown();
				}
			}
		};

		// when
		tested.execute(execution);

		// then
		assertFalse(overlap.get(), "callUnderLock allowed concurrent updates to the same step execution");
		assertEquals(threads * iterations, completed.get());
	}

	static class Listener implements StepExecutionListener {

		private LocalDateTime stepEndTime;

		@Override
		public ExitStatus afterStep(StepExecution stepExecution) {
			this.stepEndTime = stepExecution.getEndTime();
			return ExitStatus.COMPLETED;
		}

		public LocalDateTime getStepEndTime() {
			return this.stepEndTime;
		}

	}

}
