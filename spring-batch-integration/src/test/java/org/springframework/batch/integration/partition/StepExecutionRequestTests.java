/*
 * Copyright 2018-2025 the original author or authors.
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

package org.springframework.batch.integration.partition;

import java.io.IOException;

import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Mahmoud Ben Hassine
 */
class StepExecutionRequestTests {

	private static final String SERIALIZED_REQUEST = """
			{"stepName":"step","stepExecutionId":1}""";

	private final JsonMapper jsonMapper = new JsonMapper();

	@Test
	void stepExecutionRequestShouldBeSerializableWithJackson() throws IOException {
		// given
		StepExecutionRequest request = new StepExecutionRequest("step", 1L);

		// when
		String serializedRequest = this.jsonMapper.writeValueAsString(request);

		// then
		assertEquals(SERIALIZED_REQUEST, serializedRequest);
	}

	@Test
	void stepExecutionRequestShouldBeDeserializableWithJackson() throws IOException {
		// when
		StepExecutionRequest deserializedRequest = this.jsonMapper.readValue(SERIALIZED_REQUEST,
				StepExecutionRequest.class);

		// then
		assertNotNull(deserializedRequest);
		assertEquals("step", deserializedRequest.getStepName());
		assertEquals(1L, deserializedRequest.getStepExecutionId().longValue());
	}

}
