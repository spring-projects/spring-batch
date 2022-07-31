/*
 * Copyright 2006-2022 the original author or authors.
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
package org.springframework.batch.core.test.step;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletionService;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.util.SerializationUtils;

/**
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 */
class StepExecutionSerializationUtilsTests {

	@Test
	void testCycle() {
		StepExecution stepExecution = new StepExecution("step",
				new JobExecution(new JobInstance(123L, "job"), 321L, new JobParameters()), 11L);
		stepExecution.getExecutionContext().put("foo.bar.spam", 123);
		StepExecution result = SerializationUtils.clone(stepExecution);
		assertEquals(stepExecution, result);
	}

	@Test
	void testMultipleCycles() throws Throwable {

		int count = 0;
		int repeats = 100;
		int threads = 10;

		Executor executor = Executors.newFixedThreadPool(threads);
		CompletionService<StepExecution> completionService = new ExecutorCompletionService<>(executor);

		for (int i = 0; i < repeats; i++) {
			final JobExecution jobExecution = new JobExecution(new JobInstance(123L, "job"), 321L, new JobParameters());
			for (int j = 0; j < threads; j++) {
				completionService.submit(() -> {
					final StepExecution stepExecution = jobExecution.createStepExecution("step");
					stepExecution.getExecutionContext().put("foo.bar.spam", 123);
					StepExecution result = SerializationUtils.clone(stepExecution);
					assertEquals(stepExecution.getExecutionContext(), result.getExecutionContext());
					return result;
				});
			}
			for (int j = 0; j < threads; j++) {
				Future<StepExecution> future = completionService.poll(repeats, TimeUnit.MILLISECONDS);
				if (future != null) {
					count++;
					try {
						future.get();
					}
					catch (Throwable e) {
						throw new IllegalStateException("Failed on count=" + count, e);
					}
				}
			}
		}
		while (count < threads * repeats) {
			Future<StepExecution> future = completionService.poll();
			count++;
			try {
				future.get();
			}
			catch (Throwable e) {
				throw new IllegalStateException("Failed on count=" + count, e);
			}
		}
	}

}
