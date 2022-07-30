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

import org.junit.jupiter.api.Test;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.util.SerializationUtils;

/**
 * @author Dave Syer
 *
 */
class ChunkResponseTests {

	private final ChunkResponse response = new ChunkResponse(0, 111L,
			MetaDataInstanceFactory.createStepExecution().createStepContribution());

	@Test
	void testGetJobId() {
		assertEquals(Long.valueOf(111L), response.getJobId());
	}

	@Test
	void testGetStepContribution() {
		assertNotNull(response.getStepContribution());
	}

	@Test
	void testToString() {
		System.err.println(response.toString());
	}

	@Test
	void testSerializable() {
		ChunkResponse result = SerializationUtils.clone(response);
		assertNotNull(result.getStepContribution());
		assertEquals(Long.valueOf(111L), result.getJobId());
	}

}
