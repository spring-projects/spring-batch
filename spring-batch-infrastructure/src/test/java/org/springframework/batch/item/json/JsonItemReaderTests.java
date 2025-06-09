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

package org.springframework.batch.item.json;

import java.io.InputStream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Mahmoud Ben Hassine
 */
@ExtendWith(MockitoExtension.class)
class JsonItemReaderTests {

	@Mock
	private JsonObjectReader<String> jsonObjectReader;

	private JsonItemReader<String> itemReader;

	@Test
	void testValidation() {
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> new JsonItemReader<>(null, this.jsonObjectReader));
		assertEquals("The resource must not be null.", exception.getMessage());

		exception = assertThrows(IllegalArgumentException.class,
				() -> new JsonItemReader<>(new ByteArrayResource("[{}]".getBytes()), null));
		assertEquals("The json object reader must not be null.", exception.getMessage());
	}

	@Test
	void testNonExistentResource() {
		// given
		this.itemReader = new JsonItemReader<>(new NonExistentResource(), this.jsonObjectReader);

		// when
		final Exception expectedException = assertThrows(ItemStreamException.class,
				() -> this.itemReader.open(new ExecutionContext()));

		// then
		assertEquals("Failed to initialize the reader", expectedException.getMessage());
		assertTrue(expectedException.getCause() instanceof IllegalStateException);
	}

	@Test
	void testNonReadableResource() {
		// given
		this.itemReader = new JsonItemReader<>(new NonReadableResource(), this.jsonObjectReader);

		// when
		final Exception expectedException = assertThrows(ItemStreamException.class,
				() -> this.itemReader.open(new ExecutionContext()));

		// then
		assertEquals("Failed to initialize the reader", expectedException.getMessage());
		assertTrue(expectedException.getCause() instanceof IllegalStateException);
	}

	@Test
	void testReadItem() throws Exception {
		// given
		Resource resource = new ByteArrayResource("[]".getBytes());
		itemReader = new JsonItemReader<>(resource, this.jsonObjectReader);

		// when
		itemReader.open(new ExecutionContext());
		itemReader.read();

		// then
		Mockito.verify(this.jsonObjectReader).read();
	}

	private static class NonExistentResource extends AbstractResource {

		NonExistentResource() {
		}

		@Override
		public boolean exists() {
			return false;
		}

		@Override
		public String getDescription() {
			return "NonExistentResource";
		}

		@Override
		public @Nullable InputStream getInputStream() {
			return null;
		}

	}

	private static class NonReadableResource extends AbstractResource {

		NonReadableResource() {
		}

		@Override
		public boolean isReadable() {
			return false;
		}

		@Override
		public boolean exists() {
			return true;
		}

		@Override
		public String getDescription() {
			return "NonReadableResource";
		}

		@Override
		public @Nullable InputStream getInputStream() {
			return null;
		}

	}

}
