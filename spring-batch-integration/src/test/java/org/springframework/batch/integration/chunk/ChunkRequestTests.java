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
package org.springframework.batch.integration.chunk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.util.SerializationUtils;

/**
 * @author Dave Syer
 *
 */
class ChunkRequestTests {

	private final ChunkRequest<String> request = new ChunkRequest<>(0, Arrays.asList("foo", "bar"), 111L,
			MetaDataInstanceFactory.createStepExecution().createStepContribution());

	@Test
	void testGetJobId() {
		assertEquals(111L, request.getJobId());
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
		System.err.println(request.toString());
	}

	@Test
	void testSerializable() {
		ChunkRequest<String> result = SerializationUtils.clone(request);
		assertNotNull(result.getStepContribution());
		assertEquals(111L, result.getJobId());
		assertEquals(2, result.getItems().size());
	}

}
