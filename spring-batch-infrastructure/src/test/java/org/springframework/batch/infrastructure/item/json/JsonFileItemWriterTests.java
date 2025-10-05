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

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.json.JsonFileItemWriter;
import org.springframework.batch.infrastructure.item.json.JsonObjectMarshaller;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.WritableResource;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Mahmoud Ben Hassine
 */
@ExtendWith(MockitoExtension.class)
class JsonFileItemWriterTests {

	private WritableResource resource;

	@Mock
	private JsonObjectMarshaller<String> jsonObjectMarshaller;

	@BeforeEach
	void setUp() throws Exception {
		File file = Files.createTempFile("test", "json").toFile();
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

}
