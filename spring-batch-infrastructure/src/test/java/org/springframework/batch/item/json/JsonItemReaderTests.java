/*
 * Copyright 2018-2020 the original author or authors.
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

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Mahmoud Ben Hassine
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonItemReaderTests {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Mock
	private JsonObjectReader<String> jsonObjectReader;

	private JsonItemReader<String> itemReader;

	@Test
	public void testValidation() {
		try {
			new JsonItemReader<>(null, this.jsonObjectReader);
			fail("A resource is required.");
		} catch (IllegalArgumentException iae) {
			assertEquals("The resource must not be null.", iae.getMessage());
		}

		try {
			new JsonItemReader<>(new ByteArrayResource("[{}]".getBytes()), null);
			fail("A json object reader is required.");
		} catch (IllegalArgumentException iae) {
			assertEquals("The json object reader must not be null.", iae.getMessage());
		}
	}

	@Test
	public void testNonExistentResource() {
		// given
		this.expectedException.expect(ItemStreamException.class);
		this.expectedException.expectMessage("Failed to initialize the reader");
		this.expectedException.expectCause(Matchers.instanceOf(IllegalStateException.class));
		this.itemReader = new JsonItemReader<>(new NonExistentResource(), this.jsonObjectReader);

		// when
		this.itemReader.open(new ExecutionContext());

		// then
		// expected exception
	}

	@Test
	public void testNonReadableResource() {
		// given
		this.expectedException.expect(ItemStreamException.class);
		this.expectedException.expectMessage("Failed to initialize the reader");
		this.expectedException.expectCause(Matchers.instanceOf(IllegalStateException.class));
		this.itemReader = new JsonItemReader<>(new NonReadableResource(), this.jsonObjectReader);

		// when
		this.itemReader.open(new ExecutionContext());

		// then
		// expected exception
	}

	@Test
	public void testReadItem() throws Exception {
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
		public InputStream getInputStream() {
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
		public InputStream getInputStream() {
			return null;
		}
	}
}
