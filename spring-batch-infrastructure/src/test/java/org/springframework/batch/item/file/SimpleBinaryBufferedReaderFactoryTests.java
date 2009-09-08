/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.item.file;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;

import org.junit.Test;
import org.springframework.core.io.ByteArrayResource;

/**
 * @author Dave Syer
 * 
 */
public class SimpleBinaryBufferedReaderFactoryTests {

	@Test
	public void testCreate() throws Exception {
		SimpleBinaryBufferedReaderFactory factory = new SimpleBinaryBufferedReaderFactory();
		BufferedReader reader = factory.create(new ByteArrayResource("a\nb".getBytes()), "UTF-8");
		assertEquals("a", reader.readLine());
		assertEquals("b", reader.readLine());
		assertEquals(null, reader.readLine());
	}

	@Test
	public void testCreateWithLineEnding() throws Exception {
		SimpleBinaryBufferedReaderFactory factory = new SimpleBinaryBufferedReaderFactory();
		factory.setLineEnding("||");
		BufferedReader reader = factory.create(new ByteArrayResource("a||b".getBytes()), "UTF-8");
		assertEquals("a", reader.readLine());
		assertEquals("b", reader.readLine());
		assertEquals(null, reader.readLine());
	}

	@Test
	public void testMarkResetWithLineEnding() throws Exception {
		SimpleBinaryBufferedReaderFactory factory = new SimpleBinaryBufferedReaderFactory();
		factory.setLineEnding("||");
		BufferedReader reader = factory.create(new ByteArrayResource("a||b||c".getBytes()), "UTF-8");
		assertEquals("a", reader.readLine());
		reader.mark(1024);
		assertEquals("b", reader.readLine());
		reader.reset();
		assertEquals("b", reader.readLine());
		assertEquals("c", reader.readLine());
		assertEquals(null, reader.readLine());
	}

	@Test
	public void testCreateWithLineEndingAtEnd() throws Exception {
		SimpleBinaryBufferedReaderFactory factory = new SimpleBinaryBufferedReaderFactory();
		factory.setLineEnding("||");
		BufferedReader reader = factory.create(new ByteArrayResource("a||".getBytes()), "UTF-8");
		assertEquals("a", reader.readLine());
		assertEquals(null, reader.readLine());
	}

	@Test
	public void testCreateWithFalseLineEnding() throws Exception {
		SimpleBinaryBufferedReaderFactory factory = new SimpleBinaryBufferedReaderFactory();
		factory.setLineEnding("||");
		BufferedReader reader = factory.create(new ByteArrayResource("a|b||".getBytes()), "UTF-8");
		assertEquals("a|b", reader.readLine());
		assertEquals(null, reader.readLine());
	}

	@Test
	public void testCreateWithIncompleteLineEnding() throws Exception {
		SimpleBinaryBufferedReaderFactory factory = new SimpleBinaryBufferedReaderFactory();
		factory.setLineEnding("||");
		BufferedReader reader = factory.create(new ByteArrayResource("a||b|".getBytes()), "UTF-8");
		assertEquals("a", reader.readLine());
		assertEquals("b|", reader.readLine());
		assertEquals(null, reader.readLine());
	}

}
