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

package org.springframework.batch.infrastructure.item.json;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.WritableResource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Mahmoud Ben Hassine
 * @author Yanming Zhou
 */
@ExtendWith(MockitoExtension.class)
class JsonFileItemWriterTests {

	private WritableResource resource;

	@Mock
	private JsonObjectMarshaller<String> jsonObjectMarshaller;

	@BeforeEach
	void setUp() throws Exception {
		File file = Files.createTempFile("test", "json").toFile();
		file.deleteOnExit();
		this.resource = new FileSystemResource(file);
	}

	@Test
	void jsonObjectMarshallerMustNotBeNull() {
		assertThrows(IllegalArgumentException.class, () -> new JsonFileItemWriter<>(this.resource, null));
	}

	@Test
	void itemsShouldBeMarshalledToJsonWithTheJsonObjectMarshaller() throws Exception {
		// given
		JsonFileItemWriter<String> writer = new JsonFileItemWriter<>(this.resource, this.jsonObjectMarshaller);

		// when
		writer.open(new ExecutionContext());
		writer.write(Chunk.of("foo", "bar"));
		writer.close();

		// then
		Mockito.verify(this.jsonObjectMarshaller).marshal("foo");
		Mockito.verify(this.jsonObjectMarshaller).marshal("bar");
	}

	@Test
	void appendAllowed() throws Exception {
		JsonFileItemWriter<String> writer = new JsonFileItemWriter<>(this.resource,
				new JacksonJsonObjectMarshaller<>());
		writer.setAppendAllowed(true);

		writer.open(new ExecutionContext());
		writer.close();

		resourceShouldContains();

		writer.open(new ExecutionContext());
		writer.write(Chunk.of("aaa"));
		writer.write(Chunk.of("bbb"));
		writer.close();

		resourceShouldContains("aaa", "bbb");

		writer.open(new ExecutionContext());
		writer.close();

		resourceShouldContains("aaa", "bbb");

		writer.open(new ExecutionContext());
		writer.write(Chunk.of("ccc"));
		writer.close();

		resourceShouldContains("aaa", "bbb", "ccc");
	}

	@Test
	void appendNotAllowed() throws Exception {
		JsonFileItemWriter<String> writer = new JsonFileItemWriter<>(this.resource,
				new JacksonJsonObjectMarshaller<>());

		writer.open(new ExecutionContext());
		writer.close();

		resourceShouldContains();

		writer.open(new ExecutionContext());
		writer.write(Chunk.of("aaa"));
		writer.write(Chunk.of("bbb"));
		writer.close();

		resourceShouldContains("aaa", "bbb");

		writer.open(new ExecutionContext());
		writer.close();

		resourceShouldContains();

		writer.open(new ExecutionContext());
		writer.write(Chunk.of("ccc"));
		writer.close();

		resourceShouldContains("ccc");

		writer.open(new ExecutionContext());
		writer.write(Chunk.of("ddd"));
		writer.close();

		resourceShouldContains("ddd");
	}

	private void resourceShouldContains(String... array) throws Exception {
		assertArrayEquals(array, new JsonMapper().readValue(this.resource.getContentAsByteArray(), String[].class));
	}

}
