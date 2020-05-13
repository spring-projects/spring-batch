/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.batch.item.file;

import java.io.BufferedReader;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * @author Parikshit Dutta
 *
 */
public class FixedLengthBufferedReaderFactoryTests {

	private Resource inputResource =
			new ByteArrayResource("Sale012019 1   00000011000000000Sale022020 2   00000022000000000".getBytes());

	private Resource inputResource24Bytes =
			new ByteArrayResource("Sale012019 1   000000110Sale022020 2   000000220".getBytes());

	@Test
	public void testCreateWithDefaultLineLength() throws Exception {

		FixedLengthBufferedReaderFactory factory = new FixedLengthBufferedReaderFactory();

		BufferedReader reader = factory.create(inputResource, "UTF-8");
		assertEquals("Sale012019 1   00000011000000000", reader.readLine());
		assertEquals("Sale022020 2   00000022000000000", reader.readLine());
		assertEquals(null, reader.readLine());
	}

	@Test
	public void testCreateWithUserDefinedLineLength() throws Exception {

		FixedLengthBufferedReaderFactory factory = new FixedLengthBufferedReaderFactory();
		factory.setLineLength(24);

		BufferedReader reader = factory.create(inputResource24Bytes, "UTF-8");
		assertEquals("Sale012019 1   000000110", reader.readLine());
		assertEquals("Sale022020 2   000000220", reader.readLine());
		assertEquals(null, reader.readLine());
	}
}
