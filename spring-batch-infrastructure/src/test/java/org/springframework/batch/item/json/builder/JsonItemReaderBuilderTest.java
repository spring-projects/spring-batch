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

package org.springframework.batch.item.json.builder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.batch.item.json.JsonItemReader;
import org.springframework.batch.item.json.JsonObjectReader;
import org.springframework.core.io.Resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.springframework.test.util.ReflectionTestUtils.getField;

/**
 * @author Mahmoud Ben Hassine
 */
public class JsonItemReaderBuilderTest {

	@Mock
	private Resource resource;
	@Mock
	private JsonObjectReader<String> jsonObjectReader;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testValidation() {
		try {
			new JsonItemReaderBuilder<String>()
					.build();
			fail("A json object reader is required.");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("A json object reader is required.",
					iae.getMessage());
		}

		try {
			new JsonItemReaderBuilder<String>()
					.jsonObjectReader(this.jsonObjectReader)
					.build();
			fail("A name is required when saveState is set to true.");
		}
		catch (IllegalStateException iae) {
			assertEquals("A name is required when saveState is set to true.",
					iae.getMessage());
		}
	}

	@Test
	public void testConfiguration() {
		JsonItemReader<String> itemReader = new JsonItemReaderBuilder<String>()
				.jsonObjectReader(this.jsonObjectReader)
				.resource(this.resource)
				.saveState(true)
				.strict(true)
				.name("jsonItemReader")
				.maxItemCount(100)
				.currentItemCount(50)
				.build();

		Assert.assertEquals(this.jsonObjectReader, getField(itemReader, "jsonObjectReader"));
		Assert.assertEquals(this.resource, getField(itemReader, "resource"));
		Assert.assertEquals(100, getField(itemReader, "maxItemCount"));
		Assert.assertEquals(50, getField(itemReader, "currentItemCount"));
		Assert.assertTrue((Boolean) getField(itemReader, "saveState"));
		Assert.assertTrue((Boolean) getField(itemReader, "strict"));
		Object executionContext = getField(itemReader, "executionContextUserSupport");
		Assert.assertEquals("jsonItemReader", getField(executionContext, "name"));
	}

	@Test
	public void shouldBuildJsonItemReaderWhenResourceIsNotProvided(){
		JsonItemReader<String> itemReader = new JsonItemReaderBuilder<String>()
				.jsonObjectReader(this.jsonObjectReader)
				.saveState(true)
				.strict(true)
				.name("jsonItemReader")
				.maxItemCount(100)
				.currentItemCount(50)
				.build();

		Assert.assertEquals(this.jsonObjectReader, getField(itemReader, "jsonObjectReader"));
		Assert.assertEquals(100, getField(itemReader, "maxItemCount"));
		Assert.assertEquals(50, getField(itemReader, "currentItemCount"));
		Assert.assertTrue((Boolean) getField(itemReader, "saveState"));
		Assert.assertTrue((Boolean) getField(itemReader, "strict"));
		Object executionContext = getField(itemReader, "executionContextUserSupport");
		Assert.assertEquals("jsonItemReader", getField(executionContext, "name"));
	}
}
