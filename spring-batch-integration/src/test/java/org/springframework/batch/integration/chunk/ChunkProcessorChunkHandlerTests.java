/*
 * Copyright 2008-2022 the original author or authors.
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
package org.springframework.batch.integration.chunk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.item.Chunk;
import org.springframework.batch.core.step.item.ChunkProcessor;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.util.StringUtils;

class ChunkProcessorChunkHandlerTests {

	private final ChunkProcessorChunkHandler<Object> handler = new ChunkProcessorChunkHandler<>();

	protected int count = 0;

	@Test
	void testVanillaHandleChunk() throws Exception {
		handler.setChunkProcessor(new ChunkProcessor<Object>() {
			public void process(StepContribution contribution, Chunk<Object> chunk) throws Exception {
				count += chunk.size();
			}
		});
		StepContribution stepContribution = MetaDataInstanceFactory.createStepExecution().createStepContribution();
		ChunkResponse response = handler.handleChunk(
				new ChunkRequest<>(0, StringUtils.commaDelimitedListToSet("foo,bar"), 12L, stepContribution));
		assertEquals(stepContribution, response.getStepContribution());
		assertEquals(12, response.getJobId().longValue());
		assertTrue(response.isSuccessful());
		assertEquals(2, count);
	}

}
