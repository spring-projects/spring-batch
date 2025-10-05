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

package org.springframework.batch.infrastructure.item.json.builder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.json.JsonItemReader;
import org.springframework.batch.infrastructure.item.json.JsonObjectReader;
import org.springframework.batch.infrastructure.item.json.builder.JsonItemReaderBuilder;
import org.springframework.core.io.Resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.util.ReflectionTestUtils.getField;

/**
 * @author Mahmoud Ben Hassine
 */
@ExtendWith(MockitoExtension.class)
class JsonItemReaderBuilderTests {

	@Mock
	private Resource resource;

	@Mock
	private JsonObjectReader<String> jsonObjectReader;

	@Test
	void testValidation() {
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> new JsonItemReaderBuilder<String>().build());
		assertEquals("A json object reader is required.", exception.getMessage());

		exception = assertThrows(IllegalStateException.class,
				() -> new JsonItemReaderBuilder<String>().jsonObjectReader(this.jsonObjectReader).build());
		assertEquals("A name is required when saveState is set to true.", exception.getMessage());
	}

	@Test
	void testConfiguration() {
		JsonItemReader<String> itemReader = new JsonItemReaderBuilder<String>().jsonObjectReader(this.jsonObjectReader)
			.resource(this.resource)
			.saveState(true)
			.strict(true)
			.name("jsonItemReader")
			.maxItemCount(100)
			.currentItemCount(50)
			.build();

		assertEquals(this.jsonObjectReader, getField(itemReader, "jsonObjectReader"));
		assertEquals(this.resource, getField(itemReader, "resource"));
		assertEquals(100, getField(itemReader, "maxItemCount"));
		assertEquals(50, getField(itemReader, "currentItemCount"));
		assertTrue((Boolean) getField(itemReader, "saveState"));
		assertTrue((Boolean) getField(itemReader, "strict"));
		Object executionContext = getField(itemReader, "executionContextUserSupport");
		assertEquals("jsonItemReader", getField(executionContext, "name"));
	}

	@Test
	void shouldBuildJsonItemReaderWhenResourceIsNotProvided() {
		JsonItemReader<String> itemReader = new JsonItemReaderBuilder<String>().jsonObjectReader(this.jsonObjectReader)
			.saveState(true)
			.strict(true)
			.name("jsonItemReader")
			.maxItemCount(100)
			.currentItemCount(50)
			.build();

		assertEquals(this.jsonObjectReader, getField(itemReader, "jsonObjectReader"));
		assertEquals(100, getField(itemReader, "maxItemCount"));
		assertEquals(50, getField(itemReader, "currentItemCount"));
		assertTrue((Boolean) getField(itemReader, "saveState"));
		assertTrue((Boolean) getField(itemReader, "strict"));
		Object executionContext = getField(itemReader, "executionContextUserSupport");
		assertEquals("jsonItemReader", getField(executionContext, "name"));
	}

}
