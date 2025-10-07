/*
 * Copyright 2022-2023 the original author or authors.
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
package org.springframework.batch.core.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.parameters.JobParameter;

/**
 * @author Mahmoud Ben Hassine
 */
class JsonJobParametersConverterTests {

	@Test
	void testEncode() {
		// given
		JsonJobParametersConverter converter = new JsonJobParametersConverter();
		JobParameter<String> jobParameter = new JobParameter<>("name", "foo", String.class, false);

		// when
		String encodedJobParameter = converter.encode(jobParameter);

		// then
		Assertions.assertEquals(
				"{\"name\":\"name\",\"value\":\"foo\",\"type\":\"java.lang.String\",\"identifying\":\"false\"}",
				encodedJobParameter);
	}

	@Test
	void testEncodeWithDefaultIdentifyingFlag() {
		// given
		JsonJobParametersConverter converter = new JsonJobParametersConverter();
		JobParameter<String> jobParameter = new JobParameter<>("name", "foo", String.class);

		// when
		String encodedJobParameter = converter.encode(jobParameter);

		// then
		Assertions.assertEquals(
				"{\"name\":\"name\",\"value\":\"foo\",\"type\":\"java.lang.String\",\"identifying\":\"true\"}",
				encodedJobParameter);
	}

	@Test
	void testDecode() {
		// given
		JsonJobParametersConverter converter = new JsonJobParametersConverter();
		String encodedJobParameter = "{\"name\":\"name\",\"value\":\"foo\",\"type\":\"java.lang.String\",\"identifying\":\"false\"}";

		// when
		JobParameter<String> jobParameter = converter.decode("name", encodedJobParameter);

		// then
		Assertions.assertNotNull(jobParameter);
		Assertions.assertEquals("foo", jobParameter.value());
		Assertions.assertEquals(String.class, jobParameter.type());
		Assertions.assertFalse(jobParameter.identifying());
	}

	@Test
	void testDecodeWithDefaultIdentifyingFlag() {
		// given
		JsonJobParametersConverter converter = new JsonJobParametersConverter();
		String encodedJobParameter = "{\"name\":\"name\",\"value\":\"foo\",\"type\":\"java.lang.String\"}";

		// when
		JobParameter<String> jobParameter = converter.decode("name", encodedJobParameter);

		// then
		Assertions.assertNotNull(jobParameter);
		Assertions.assertEquals("foo", jobParameter.value());
		Assertions.assertEquals(String.class, jobParameter.type());
		Assertions.assertTrue(jobParameter.identifying());
	}

	@Test
	void testDecodeWithDefaultIdentifyingFlagAndDefaultType() {
		// given
		JsonJobParametersConverter converter = new JsonJobParametersConverter();
		String encodedJobParameter = "{\"name\":\"name\",\"value\":\"foo\"}";

		// when
		JobParameter<String> jobParameter = converter.decode("name", encodedJobParameter);

		// then
		Assertions.assertNotNull(jobParameter);
		Assertions.assertEquals("name", jobParameter.name());
		Assertions.assertEquals("foo", jobParameter.value());
		Assertions.assertEquals(String.class, jobParameter.type());
		Assertions.assertTrue(jobParameter.identifying());
	}

}