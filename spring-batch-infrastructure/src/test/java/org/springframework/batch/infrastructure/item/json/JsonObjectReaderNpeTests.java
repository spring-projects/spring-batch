/*
 * Copyright 2018-2025 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for NPE fix in JsonObjectReader close() methods.
 *
 * @author nikitanagar08
 */
class JsonObjectReaderNpeTests {

	@Test
	void testJacksonJsonItemReaderNpeWhenOpenFails() throws Exception {
		Resource resource = new FailingResource();
		JacksonJsonObjectReader<String> jsonObjectReader = new JacksonJsonObjectReader<>(String.class);
		JsonItemReader<String> itemReader = new JsonItemReader<>(resource, jsonObjectReader);

		assertThrows(ItemStreamException.class, () -> itemReader.open(new ExecutionContext()));
		// close() should not throw NPE
		assertDoesNotThrow(() -> itemReader.close());
	}

	@Test
	void testGsonJsonItemReaderNpeWhenOpenFails() throws Exception {
		Resource resource = new FailingResource();
		GsonJsonObjectReader<String> jsonObjectReader = new GsonJsonObjectReader<>(String.class);
		JsonItemReader<String> itemReader = new JsonItemReader<>(resource, jsonObjectReader);

		assertThrows(ItemStreamException.class, () -> itemReader.open(new ExecutionContext()));
		// close() should not throw NPE
		assertDoesNotThrow(() -> itemReader.close());
	}

	private static class FailingResource extends AbstractResource {

		@Override
		public String getDescription() {
			return "fail";
		}

		@Override
		public InputStream getInputStream() throws IOException {
			throw new IOException("Connection failed during open");
		}

	}

}
