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

package org.springframework.batch.item.json.builder;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.batch.item.file.FlatFileFooterCallback;
import org.springframework.batch.item.file.FlatFileHeaderCallback;
import org.springframework.batch.item.json.JsonFileItemWriter;
import org.springframework.batch.item.json.JsonObjectMarshaller;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.WritableResource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Mahmoud Ben Hassine
 * @author Glenn Renfro
 */
class JsonFileItemWriterBuilderTests {

	private WritableResource resource;

	private JsonObjectMarshaller<String> jsonObjectMarshaller;

	@BeforeEach
	void setUp() throws Exception {
		File file = Files.createTempFile("test", "json").toFile();
		this.resource = new FileSystemResource(file);
		this.jsonObjectMarshaller = object -> object;
	}

	@Test
	void testMissingResource() {
		var builder = new JsonFileItemWriterBuilder<String>().jsonObjectMarshaller(this.jsonObjectMarshaller);
		assertThrows(IllegalArgumentException.class, builder::build);
	}

	@Test
	void testMissingJsonObjectMarshaller() {
		var builder = new JsonFileItemWriterBuilder<String>().resource(this.resource);
		assertThrows(IllegalArgumentException.class, builder::build);
	}

	@Test
	void testMandatoryNameWhenSaveStateIsSet() {
		var builder = new JsonFileItemWriterBuilder<String>().resource(this.resource)
			.jsonObjectMarshaller(this.jsonObjectMarshaller);
		assertThrows(IllegalArgumentException.class, builder::build);
	}

	@Test
	void testJsonFileItemWriterCreation() {
		// given
		boolean append = true;
		boolean forceSync = true;
		boolean transactional = true;
		boolean shouldDeleteIfEmpty = true;
		boolean shouldDeleteIfExists = true;
		String encoding = "UTF-8";
		String lineSeparator = "#";
		FlatFileHeaderCallback headerCallback = Mockito.mock(FlatFileHeaderCallback.class);
		FlatFileFooterCallback footerCallback = Mockito.mock(FlatFileFooterCallback.class);

		// when
		JsonFileItemWriter<String> writer = new JsonFileItemWriterBuilder<String>().name("jsonFileItemWriter")
			.resource(this.resource)
			.jsonObjectMarshaller(this.jsonObjectMarshaller)
			.append(append)
			.encoding(encoding)
			.forceSync(forceSync)
			.headerCallback(headerCallback)
			.footerCallback(footerCallback)
			.lineSeparator(lineSeparator)
			.shouldDeleteIfEmpty(shouldDeleteIfEmpty)
			.shouldDeleteIfExists(shouldDeleteIfExists)
			.transactional(transactional)
			.build();

		// then
		validateBuilderFlags(writer, encoding, lineSeparator, headerCallback, footerCallback);
	}

	@Test
	void testJsonFileItemWriterCreationDefaultEncoding() {
		// given
		boolean append = true;
		boolean forceSync = true;
		boolean transactional = true;
		boolean shouldDeleteIfEmpty = true;
		boolean shouldDeleteIfExists = true;
		String encoding = Charset.defaultCharset().name();
		String lineSeparator = "#";
		FlatFileHeaderCallback headerCallback = Mockito.mock(FlatFileHeaderCallback.class);
		FlatFileFooterCallback footerCallback = Mockito.mock(FlatFileFooterCallback.class);

		// when
		JsonFileItemWriter<String> writer = new JsonFileItemWriterBuilder<String>().name("jsonFileItemWriter")
			.resource(this.resource)
			.jsonObjectMarshaller(this.jsonObjectMarshaller)
			.append(append)
			.forceSync(forceSync)
			.headerCallback(headerCallback)
			.footerCallback(footerCallback)
			.lineSeparator(lineSeparator)
			.shouldDeleteIfEmpty(shouldDeleteIfEmpty)
			.shouldDeleteIfExists(shouldDeleteIfExists)
			.transactional(transactional)
			.build();

		// then
		validateBuilderFlags(writer, encoding, lineSeparator, headerCallback, footerCallback);
	}

	private void validateBuilderFlags(JsonFileItemWriter<String> writer, String encoding, String lineSeparator,
			FlatFileHeaderCallback headerCallback, FlatFileFooterCallback footerCallback) {
		assertTrue((Boolean) ReflectionTestUtils.getField(writer, "saveState"));
		assertTrue((Boolean) ReflectionTestUtils.getField(writer, "append"));
		assertTrue((Boolean) ReflectionTestUtils.getField(writer, "transactional"));
		assertTrue((Boolean) ReflectionTestUtils.getField(writer, "shouldDeleteIfEmpty"));
		assertTrue((Boolean) ReflectionTestUtils.getField(writer, "shouldDeleteIfExists"));
		assertTrue((Boolean) ReflectionTestUtils.getField(writer, "forceSync"));
		assertEquals(encoding, ReflectionTestUtils.getField(writer, "encoding"));
		assertEquals(lineSeparator, ReflectionTestUtils.getField(writer, "lineSeparator"));
		assertEquals(headerCallback, ReflectionTestUtils.getField(writer, "headerCallback"));
		assertEquals(footerCallback, ReflectionTestUtils.getField(writer, "footerCallback"));
	}

}
