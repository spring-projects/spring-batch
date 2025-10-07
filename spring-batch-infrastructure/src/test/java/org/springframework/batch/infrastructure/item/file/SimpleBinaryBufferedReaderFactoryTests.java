/*
 * Copyright 2006-2024 the original author or authors.
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
package org.springframework.batch.infrastructure.item.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.BufferedReader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.batch.infrastructure.item.file.SimpleBinaryBufferedReaderFactory;
import org.springframework.core.io.ByteArrayResource;

/**
 * @author Dave Syer
 *
 */
class SimpleBinaryBufferedReaderFactoryTests {

	@Test
	void testCreate() throws Exception {
		SimpleBinaryBufferedReaderFactory factory = new SimpleBinaryBufferedReaderFactory();
		@SuppressWarnings("resource")
		BufferedReader reader = factory.create(new ByteArrayResource("a\nb".getBytes()), "UTF-8");
		assertEquals("a", reader.readLine());
		assertEquals("b", reader.readLine());
		assertNull(reader.readLine());
	}

	@Test
	void testCreateWithLineEnding() throws Exception {
		SimpleBinaryBufferedReaderFactory factory = new SimpleBinaryBufferedReaderFactory();
		factory.setLineEnding("||");
		@SuppressWarnings("resource")
		BufferedReader reader = factory.create(new ByteArrayResource("a||b".getBytes()), "UTF-8");
		assertEquals("a", reader.readLine());
		assertEquals("b", reader.readLine());
		assertNull(reader.readLine());
	}

	@Test
	void testMarkResetWithLineEnding() throws Exception {
		SimpleBinaryBufferedReaderFactory factory = new SimpleBinaryBufferedReaderFactory();
		factory.setLineEnding("||");
		@SuppressWarnings("resource")
		BufferedReader reader = factory.create(new ByteArrayResource("a||b||c".getBytes()), "UTF-8");
		assertEquals("a", reader.readLine());
		reader.mark(1024);
		assertEquals("b", reader.readLine());
		reader.reset();
		assertEquals("b", reader.readLine());
		assertEquals("c", reader.readLine());
		assertNull(reader.readLine());
	}

	@Test
	void testCreateWithLineEndingAtEnd() throws Exception {
		SimpleBinaryBufferedReaderFactory factory = new SimpleBinaryBufferedReaderFactory();
		factory.setLineEnding("||");
		@SuppressWarnings("resource")
		BufferedReader reader = factory.create(new ByteArrayResource("a||".getBytes()), "UTF-8");
		assertEquals("a", reader.readLine());
		assertNull(reader.readLine());
	}

	@ParameterizedTest
	@ValueSource(strings = { "||", "|||" })
	void testCreateWithFalseLineEnding(String lineEnding) throws Exception {
		SimpleBinaryBufferedReaderFactory factory = new SimpleBinaryBufferedReaderFactory();
		factory.setLineEnding(lineEnding);
		@SuppressWarnings("resource")
		BufferedReader reader = factory.create(new ByteArrayResource(("a|b" + lineEnding).getBytes()), "UTF-8");
		assertEquals("a|b", reader.readLine());
		assertNull(reader.readLine());
	}

	@Test
	void testCreateWithFalseMixedCharacterLineEnding() throws Exception {
		SimpleBinaryBufferedReaderFactory factory = new SimpleBinaryBufferedReaderFactory();
		factory.setLineEnding("#@");
		@SuppressWarnings("resource")
		BufferedReader reader = factory.create(new ByteArrayResource("a##@".getBytes()), "UTF-8");
		assertEquals("a#", reader.readLine());
		assertNull(reader.readLine());
	}

	@Test
	void testCreateWithIncompleteLineEnding() throws Exception {
		SimpleBinaryBufferedReaderFactory factory = new SimpleBinaryBufferedReaderFactory();
		factory.setLineEnding("||");
		@SuppressWarnings("resource")
		BufferedReader reader = factory.create(new ByteArrayResource("a||b|".getBytes()), "UTF-8");
		assertEquals("a", reader.readLine());
		assertEquals("b|", reader.readLine());
		assertNull(reader.readLine());
	}

}
