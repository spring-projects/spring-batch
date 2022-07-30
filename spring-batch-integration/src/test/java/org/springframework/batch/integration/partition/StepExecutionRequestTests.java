/*
 * Copyright 2018-2022 the original author or authors.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Mahmoud Ben Hassine
 */
class StepExecutionRequestTests {

	private static final String SERIALIZED_REQUEST = "{\"stepExecutionId\":1,\"stepName\":\"step\",\"jobExecutionId\":1}";

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void stepExecutionRequestShouldBeSerializableWithJackson() throws IOException {
		// given
		StepExecutionRequest request = new StepExecutionRequest("step", 1L, 1L);

		// when
		String serializedRequest = this.objectMapper.writeValueAsString(request);

		// then
		assertEquals(SERIALIZED_REQUEST, serializedRequest);
	}

	@Test
	void stepExecutionRequestShouldBeDeserializableWithJackson() throws IOException {
		// when
		StepExecutionRequest deserializedRequest = this.objectMapper.readValue(SERIALIZED_REQUEST,
				StepExecutionRequest.class);

		// then
		assertNotNull(deserializedRequest);
		assertEquals("step", deserializedRequest.getStepName());
		assertEquals(1L, deserializedRequest.getJobExecutionId().longValue());
		assertEquals(1L, deserializedRequest.getStepExecutionId().longValue());
	}

}
