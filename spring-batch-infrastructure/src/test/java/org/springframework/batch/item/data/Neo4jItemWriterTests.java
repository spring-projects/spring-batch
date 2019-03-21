/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.batch.item.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.neo4j.template.Neo4jOperations;

public class Neo4jItemWriterTests {

	private Neo4jItemWriter<String> writer;
	@Mock
	private Neo4jOperations template;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		writer = new Neo4jItemWriter<String>();

		writer.setTemplate(template);
	}

	@Test
	public void testAfterPropertiesSet() throws Exception{
		writer = new Neo4jItemWriter<String>();

		try {
			writer.afterPropertiesSet();
			fail("Template was not set but exception was not thrown.");
		} catch (IllegalStateException iae) {
			assertEquals("A Neo4JOperations implementation is required", iae.getMessage());
		} catch (Throwable t) {
			fail("Wrong exception was thrown.");
		}

		writer.setTemplate(template);

		writer.afterPropertiesSet();
	}

	@Test
	public void testWriteNull() throws Exception {
		writer.write(null);

		verifyZeroInteractions(template);
	}

	@Test
	public void testWriteNoItems() throws Exception {
		writer.write(new ArrayList<String>());

		verifyZeroInteractions(template);
	}

	@Test
	public void testWriteItems() throws Exception {
		List<String> items = new ArrayList<String>();
		items.add("foo");
		items.add("bar");

		writer.write(items);

		verify(template).save("foo");
		verify(template).save("bar");
	}

	@Test
	public void testDeleteItems() throws Exception {
		List<String> items = new ArrayList<String>();
		items.add("foo");
		items.add("bar");

		writer.setDelete(true);

		writer.write(items);

		verify(template).delete("foo");
		verify(template).delete("bar");
	}
}
