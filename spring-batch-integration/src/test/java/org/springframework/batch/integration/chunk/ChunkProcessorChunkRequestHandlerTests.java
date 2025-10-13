/*
 * Copyright 2008-2025 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.test.MetaDataInstanceFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkProcessorChunkRequestHandlerTests {

	private final ChunkProcessorChunkRequestHandler<Object> handler = new ChunkProcessorChunkRequestHandler<>();

	protected int count = 0;

	@Test
	void testVanillaHandleChunk() throws Exception {
		// given
		handler.setChunkProcessor((chunk, contribution) -> count += chunk.size());
		StepContribution stepContribution = MetaDataInstanceFactory.createStepExecution().createStepContribution();
		Chunk items = Chunk.of("foo", "bar");
		ChunkRequest chunkRequest = new ChunkRequest<>(0, items, 12L, stepContribution);

		// when
		ChunkResponse response = handler.handle(chunkRequest);

		// then
		assertEquals(stepContribution, response.getStepContribution());
		assertEquals(12, response.getJobInstanceId().longValue());
		assertTrue(response.isSuccessful());
		assertEquals(2, count);
	}

}
