/*
 * Copyright 2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.item.data.builder;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.data.mongodb.core.MongoOperations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Glenn Renfro
 */
public class MongoItemWriterBuilderTests {
	@Mock
	private MongoOperations template;

	private List<String> items;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		this.items = Arrays.asList("foo", "bar");
	}

	@Test
	public void testBasicWrite() throws Exception {
		MongoItemWriter<String> writer = new MongoItemWriterBuilder<String>().template(this.template).build();
		writer.write(this.items);

		verify(this.template).save(this.items.get(0));
		verify(this.template).save(this.items.get(1));
		verify(this.template, never()).remove(this.items.get(0));
		verify(this.template, never()).remove(this.items.get(1));
	}

	@Test
	public void testDelete() throws Exception {
		MongoItemWriter<String> writer = new MongoItemWriterBuilder<String>().template(this.template)
				.delete(true)
				.build();

		writer.write(this.items);

		verify(this.template).remove(this.items.get(0));
		verify(this.template).remove(this.items.get(1));
		verify(this.template, never()).save(this.items.get(0));
		verify(this.template, never()).save(this.items.get(1));
	}

	@Test
	public void testWriteToCollection() throws Exception {
		MongoItemWriter<String> writer = new MongoItemWriterBuilder<String>().collection("collection")
				.template(this.template)
				.build();

		writer.write(this.items);

		verify(this.template).save(this.items.get(0), "collection");
		verify(this.template).save(this.items.get(1), "collection");
		verify(this.template, never()).remove(this.items.get(0), "collection");
		verify(this.template, never()).remove(this.items.get(1), "collection");
	}

	@Test
	public void testNullTemplate() {
		try {
			new MongoItemWriterBuilder<>().build();
			fail("IllegalArgumentException should have been thrown");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("IllegalArgumentException message did not match the expected result.", "template is required.",
					iae.getMessage());
		}
	}
}
