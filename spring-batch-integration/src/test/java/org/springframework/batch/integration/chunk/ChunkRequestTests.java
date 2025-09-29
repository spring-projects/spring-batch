/*
 * Copyright 2006-2023 the original author or authors.
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
package org.springframework.batch.integration.chunk;

import org.junit.jupiter.api.Test;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.util.SerializationUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class ChunkRequestTests {

	private final ChunkRequest<String> request = new ChunkRequest<>(0, Chunk.of("foo", "bar"), 111L,
			MetaDataInstanceFactory.createStepExecution().createStepContribution());

	@Test
	void testGetJobId() {
		assertEquals(111L, request.getJobInstanceId());
	}

	@Test
	void testGetItems() {
		assertEquals(2, request.getItems().size());
	}

	@Test
	void testGetStepContribution() {
		assertNotNull(request.getStepContribution());
	}

	@Test
	void testToString() {
		assertEquals(
				"ChunkRequest: jobId=111, sequence=0, contribution=[StepContribution: read=0, written=0, filtered=0, readSkips=0, writeSkips=0, processSkips=0, exitStatus=EXECUTING], item count=2",
				request.toString());
	}

	@Test
	void testSerializable() {
		ChunkRequest<String> result = SerializationUtils.clone(request);
		assertNotNull(result.getStepContribution());
		assertEquals(111L, result.getJobInstanceId());
		assertEquals(2, result.getItems().size());
	}

}
