/*
 * Copyright 2006-2022 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

/**
 * @author Dave Syer
 *
 */
class DefaultBufferedReaderFactoryTests {

	@Test
	void testCreate() throws Exception {
		DefaultBufferedReaderFactory factory = new DefaultBufferedReaderFactory();
		@SuppressWarnings("resource")
		BufferedReader reader = factory.create(new ByteArrayResource("a\nb\nc".getBytes()), "UTF-8");
		assertEquals("a", reader.readLine());
	}

}
