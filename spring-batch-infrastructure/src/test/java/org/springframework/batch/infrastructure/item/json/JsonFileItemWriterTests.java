/*
 * Copyright 2018-2026 the original author or authors.
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

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

		resourceShouldContain();

		writer.open(new ExecutionContext());
		writer.write(Chunk.of("aaa"));
		writer.write(Chunk.of("bbb"));
		writer.close();

		resourceShouldContain("aaa", "bbb");

		writer.open(new ExecutionContext());
		writer.close();

		resourceShouldContain("aaa", "bbb");

		writer.open(new ExecutionContext());
		writer.write(Chunk.of("ccc"));
		writer.close();

		resourceShouldContain("aaa", "bbb", "ccc");
	}

	@Test
	void appendAllowedWithUnformattedJson() throws Exception {
		JsonFileItemWriter<String> writer = new JsonFileItemWriter<>(this.resource,
				new JacksonJsonObjectMarshaller<>());
		writer.setAppendAllowed(true);

		Files.writeString(this.resource.getFilePath(), "[\n \"foo\"]", StandardCharsets.UTF_8);

		resourceShouldContain("foo");

		writer.open(new ExecutionContext());
		writer.write(Chunk.of("bar"));
		writer.close();

		resourceShouldContain("foo", "bar");
	}

	@Test
	void appendAllowedWithCustomHeaderAndFooter() throws Exception {
		JsonFileItemWriter<String> writer = new JsonFileItemWriter<>(this.resource,
				new JacksonJsonObjectMarshaller<>());
		writer.setAppendAllowed(true);
		writer.setHeaderCallback(headerWriter -> headerWriter.write("{\"entries\":["));
		writer.setFooterCallback(footerWriter -> footerWriter.write("]}"));

		writer.open(new ExecutionContext());
		writer.close();

		wrappedResourceShouldContain();

		writer.open(new ExecutionContext());
		writer.write(Chunk.of("foo"));
		writer.close();

		wrappedResourceShouldContain("foo");

		writer.open(new ExecutionContext());
		writer.write(Chunk.of("bar"));
		writer.close();

		wrappedResourceShouldContain("foo", "bar");
	}

	@Test
	void appendAllowedWithExistingContentContainingArrayBeforeCustomArray() throws Exception {
		JsonFileItemWriter<String> writer = new JsonFileItemWriter<>(this.resource,
				new JacksonJsonObjectMarshaller<>());
		writer.setAppendAllowed(true);
		writer.setFooterCallback(footerWriter -> footerWriter.write("]}"));

		Files.writeString(this.resource.getFilePath(), "{\"existing\": [1, 2], \"entries\":[", StandardCharsets.UTF_8);

		writer.open(new ExecutionContext());
		writer.close();

		writer.open(new ExecutionContext());
		writer.write(Chunk.of("foo"));
		writer.close();

		wrappedResourceShouldContain(List.of(1, 2), "foo");
	}

	@Test
	void appendAllowedWithCustomFooterContainingArrayStartInString() throws Exception {
		JsonFileItemWriter<String> writer = new JsonFileItemWriter<>(this.resource,
				new JacksonJsonObjectMarshaller<>());
		writer.setAppendAllowed(true);
		writer.setHeaderCallback(headerWriter -> headerWriter.write("{\"entries\":["));
		writer.setFooterCallback(footerWriter -> footerWriter.write("],\"status\":\"[pending\"}"));

		writer.open(new ExecutionContext());
		writer.write(Chunk.of("foo"));
		writer.close();

		writer.open(new ExecutionContext());
		writer.write(Chunk.of("bar"));
		writer.close();

		wrappedResourceShouldContainStatus("[pending", "foo", "bar");
	}

	@Test
	void appendNotAllowed() throws Exception {
		JsonFileItemWriter<String> writer = new JsonFileItemWriter<>(this.resource,
				new JacksonJsonObjectMarshaller<>());

		writer.open(new ExecutionContext());
		writer.close();

		resourceShouldContain();

		writer.open(new ExecutionContext());
		writer.write(Chunk.of("aaa"));
		writer.write(Chunk.of("bbb"));
		writer.close();

		resourceShouldContain("aaa", "bbb");

		writer.open(new ExecutionContext());
		writer.close();

		resourceShouldContain();

		writer.open(new ExecutionContext());
		writer.write(Chunk.of("ccc"));
		writer.close();

		resourceShouldContain("ccc");

		writer.open(new ExecutionContext());
		writer.write(Chunk.of("ddd"));
		writer.close();

		resourceShouldContain("ddd");
	}

	private void resourceShouldContain(String... array) throws Exception {
		assertArrayEquals(array, new JsonMapper().readValue(this.resource.getContentAsByteArray(), String[].class));
	}

	@SuppressWarnings("unchecked")
	private void wrappedResourceShouldContain(String... entries) throws Exception {
		Map<String, Object> wrapper = new JsonMapper().readValue(this.resource.getContentAsByteArray(), Map.class);
		assertEquals(List.of(entries), wrapper.get("entries"));
	}

	@SuppressWarnings("unchecked")
	private void wrappedResourceShouldContain(List<Integer> existing, String... entries) throws Exception {
		Map<String, Object> wrapper = new JsonMapper().readValue(this.resource.getContentAsByteArray(), Map.class);
		assertEquals(existing, wrapper.get("existing"));
		assertEquals(List.of(entries), wrapper.get("entries"));
	}

	@SuppressWarnings("unchecked")
	private void wrappedResourceShouldContainStatus(String status, String... entries) throws Exception {
		Map<String, Object> wrapper = new JsonMapper().readValue(this.resource.getContentAsByteArray(), Map.class);
		assertEquals(status, wrapper.get("status"));
		assertEquals(List.of(entries), wrapper.get("entries"));
	}

}
