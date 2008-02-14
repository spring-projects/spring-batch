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
package org.springframework.batch.execution.step.simple;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.ChunkingResult;
import org.springframework.batch.core.domain.ItemSkipPolicy;
import org.springframework.batch.core.domain.StepExecution;

public class ItemChunkerTests extends TestCase {
	
	StepExecution stepExecution;
	
	protected void setUp() throws Exception {
		super.setUp();
		
		stepExecution = new StepExecution(null,null);
	}
	
	public void testSizeNegative() {
		try {
			MockItemReader itemReader = new MockItemReader(10);
			ItemChunker chunkReader = new ItemChunker(itemReader);
			chunkReader.chunk(-1, stepExecution);
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	public void testSizeZero() {
		try {
			MockItemReader itemReader = new MockItemReader(10);
			ItemChunker chunkReader = new ItemChunker(itemReader);
			chunkReader.chunk(0, stepExecution);
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	public void testSizePositive() {
		MockItemReader itemReader = new MockItemReader(10);
		ItemChunker chunkReader = new ItemChunker(itemReader);
		ChunkingResult chunkingResult = chunkReader.chunk(10, stepExecution);
		assertEquals(10, chunkingResult.getChunk().getItems().size());
	}

	public void testIncompleteChunk() {
		MockItemReader itemReader = new MockItemReader(5);
		ItemChunker chunkReader = new ItemChunker(itemReader);
		ChunkingResult chunkingResult = chunkReader.chunk(10, stepExecution);
		assertEquals(5, chunkingResult.getChunk().getItems().size());
	}

	public void testPolicyNoContinue() {
		MockItemReader itemReader = new MockItemReader(1);
		itemReader.setFail(true);
		ItemChunker chunkReader = new ItemChunker(itemReader);
		chunkReader.setItemSkipPolicy(new StubReadFailurePolicy(true));
		try {
			chunkReader.chunk(10, stepExecution);
			fail();
		} catch (RuntimeException e) {
		}
	}

	public void testPolicyContinueWithFailure() {
		MockItemReader itemReader = new MockItemReader(1);
		itemReader.setFail(true);
		ItemChunker chunkReader = new ItemChunker(itemReader);
		chunkReader.setItemSkipPolicy(new StubReadFailurePolicy(false));
		ChunkingResult chunkingResult = chunkReader.chunk(1, stepExecution);
		assertEquals(1,chunkingResult.getChunk().getItems().size());
	}

	private class StubReadFailurePolicy implements ItemSkipPolicy {

		private final boolean fail;

		public StubReadFailurePolicy(boolean fail) {
			this.fail = fail;
		}

		public boolean shouldSkip(Exception ex, StepExecution stepExecution) {
			return !fail;
		}
	}
}
