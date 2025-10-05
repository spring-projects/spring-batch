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
package org.springframework.batch.core.scope.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.repeat.RepeatContext;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

/**
 * @author Dave Syer
 *
 */
class StepContextRepeatCallbackTests {

	private final StepExecution stepExecution = new StepExecution(123L, "foo",
			new JobExecution(0L, new JobInstance(1L, "job"), new JobParameters()));

	private boolean addedAttribute = false;

	private boolean removedAttribute = false;

	@AfterEach
	void cleanUpStepContext() {
		StepSynchronizationManager.close();
	}

	@Test
	void testDoInIteration() throws Exception {
		StepContextRepeatCallback callback = new StepContextRepeatCallback(stepExecution) {
			@Override
			public RepeatStatus doInChunkContext(RepeatContext context, ChunkContext chunkContext) throws Exception {
				assertEquals(Long.valueOf(123), chunkContext.getStepContext().getStepExecution().getId());
				return RepeatStatus.FINISHED;
			}
		};
		assertEquals(RepeatStatus.FINISHED, callback.doInIteration(null));
		assertEquals(ExitStatus.EXECUTING, stepExecution.getExitStatus());
	}

	@Test
	void testAddingAttributes() throws Exception {
		StepSynchronizationManager.register(stepExecution);
		StepContextRepeatCallback callback = new StepContextRepeatCallback(stepExecution) {
			@Override
			public RepeatStatus doInChunkContext(RepeatContext context, ChunkContext chunkContext) throws Exception {
				if (addedAttribute) {
					removedAttribute = chunkContext.hasAttribute("foo");
					chunkContext.removeAttribute("foo");
				}
				else {
					addedAttribute = true;
					chunkContext.setAttribute("foo", "bar");
				}
				return RepeatStatus.FINISHED;
			}
		};
		assertEquals(RepeatStatus.FINISHED, callback.doInIteration(null));
		assertTrue(addedAttribute);
		callback.doInIteration(null);
		assertTrue(removedAttribute);
		callback.doInIteration(null);
		assertFalse(removedAttribute);
	}

}
