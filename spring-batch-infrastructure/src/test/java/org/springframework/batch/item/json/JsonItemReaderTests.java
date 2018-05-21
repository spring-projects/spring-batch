/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.core.io.AbstractResource;

/**
 * @author Mahmoud Ben Hassine
 */
public class JsonItemReaderTests {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	private JsonItemReader<String> itemReader = new JsonItemReader<>();

	@Test
	public void testMandatoryJsonObjectReader() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("The json object reader must not be null.");

		this.itemReader.afterPropertiesSet();
	}

	@Test
	public void testMandatoryResource() throws Exception {
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("The resource must not be null.");
		this.itemReader.setJsonObjectReader(() -> null);

		this.itemReader.afterPropertiesSet();
	}

	@Test
	public void testNonExistentResource() throws Exception {
		this.expectedException.expect(ItemStreamException.class);
		this.expectedException.expectMessage("Failed to initialize the reader");
		this.expectedException.expectCause(Matchers.instanceOf(IllegalStateException.class));
		this.itemReader.setJsonObjectReader(() -> null);
		this.itemReader.setResource(new NonExistentResource());
		this.itemReader.afterPropertiesSet();

		this.itemReader.open(new ExecutionContext());
	}

	@Test
	public void testNonReadableResource() throws Exception {
		this.expectedException.expect(ItemStreamException.class);
		this.expectedException.expectMessage("Failed to initialize the reader");
		this.expectedException.expectCause(Matchers.instanceOf(IllegalStateException.class));
		this.itemReader.setJsonObjectReader(() -> null);
		this.itemReader.setResource(new NonReadableResource());
		this.itemReader.afterPropertiesSet();

		this.itemReader.open(new ExecutionContext());
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
