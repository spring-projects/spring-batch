/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.scope.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * @author Dave Syer
 * 
 */
public class StepContextRepeatCallbackTests {
	
	private StepExecution stepExecution = new StepExecution("foo", new JobExecution(0L), 123L);
	private boolean addedAttribute = false;
	private boolean removedAttribute = false;
	
	@After
	public void cleanUpStepContext() {
		StepSynchronizationManager.close();
	}

	@Test
	public void testDoInIteration() throws Exception {
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
	public void testAddingAttributes() throws Exception {
		StepSynchronizationManager.register(stepExecution);
		StepContextRepeatCallback callback = new StepContextRepeatCallback(stepExecution) {
			@Override
			public RepeatStatus doInChunkContext(RepeatContext context, ChunkContext chunkContext) throws Exception {
				if (addedAttribute) {
					removedAttribute = chunkContext.hasAttribute("foo");
					chunkContext.removeAttribute("foo");
				} else {
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
