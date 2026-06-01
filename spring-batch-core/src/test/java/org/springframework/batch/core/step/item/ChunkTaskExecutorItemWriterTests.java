/*
 * Copyright 2025-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.step.item;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ChunkTaskExecutorItemWriterTests {

	@Test
	void stepContextIsPropagatedToWorkerThread() throws Exception {
		// given
		StepExecution stepExecution = createStepExecution();
		AtomicReference<StepContext> capturedContext = new AtomicReference<>();
		ChunkProcessor<String> chunkProcessor = (chunk, contribution) -> {
			capturedContext.set(StepSynchronizationManager.getContext());
			contribution.setExitStatus(ExitStatus.COMPLETED);
		};
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(1);
		taskExecutor.setThreadNamePrefix("worker-thread-");
		taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
		taskExecutor.afterPropertiesSet();
		try {
			ChunkTaskExecutorItemWriter<String> itemWriter = new ChunkTaskExecutorItemWriter<>(chunkProcessor,
					taskExecutor);
			itemWriter.beforeStep(stepExecution);

			// when
			itemWriter.write(Chunk.of("foo", "bar"));
			ExitStatus exitStatus = itemWriter.afterStep(stepExecution);

			// then
			assertEquals(ExitStatus.COMPLETED.getExitCode(), exitStatus.getExitCode());
			StepContext context = capturedContext.get();
			assertNotNull(context, "StepContext should be available on the worker thread");
			assertEquals(stepExecution, context.getStepExecution());
		}
		finally {
			taskExecutor.shutdown();
		}
	}

	private StepExecution createStepExecution() {
		return new StepExecution("step", new JobExecution(1, new JobInstance(1, "job"), new JobParameters()));
	}

}
