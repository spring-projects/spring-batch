/*
 * Copyright 2013-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import org.mockito.MockitoAnnotations;

import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

/**
 * @author Michael Minella
 * @author Parikshit Dutta
 * @author Mahmoud Ben Hassine
 */
@SuppressWarnings("serial")
public class MongoItemWriterTests {

	private MongoItemWriter<Object> writer;
	@Mock
	private MongoOperations template;
	@Mock
	private BulkOperations bulkOperations;
	@Mock
	DbRefResolver dbRefResolver;

	private PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		when(this.template.bulkOps(any(), anyString())).thenReturn(this.bulkOperations);
		when(this.template.bulkOps(any(), any(Class.class))).thenReturn(this.bulkOperations);

		MappingContext mappingContext = new MongoMappingContext();
		MappingMongoConverter mongoConverter = spy(new MappingMongoConverter(this.dbRefResolver, mappingContext));
		when(this.template.getConverter()).thenReturn(mongoConverter);

		writer = new MongoItemWriter<>();
		writer.setTemplate(template);
		writer.afterPropertiesSet();
	}

	@Test
	public void testAfterPropertiesSet() throws Exception {
		writer = new MongoItemWriter<>();

		try {
			writer.afterPropertiesSet();
			fail("Expected exception was not thrown");
		} catch (IllegalStateException ignore) {
		}

		writer.setTemplate(template);
		writer.afterPropertiesSet();
	}

	@Test
	public void testWriteNoTransactionNoCollection() throws Exception {
		List<Item> items = new ArrayList<Item>() {{
			add(new Item("Foo"));
			add(new Item("Bar"));
		}};

		writer.write(items);

		verify(template).bulkOps(any(), any(Class.class));
		verify(bulkOperations, times(2)).replaceOne(any(Query.class), any(Object.class), any());
	}

	@Test
	public void testWriteNoTransactionWithCollection() throws Exception {
		List<Object> items = new ArrayList<Object>() {{
			add(new Item("Foo"));
			add(new Item("Bar"));
		}};

		writer.setCollection("collection");

		writer.write(items);

		verify(template).bulkOps(any(), eq("collection"));
		verify(bulkOperations, times(2)).replaceOne(any(Query.class), any(Object.class), any());
	}

	@Test
	public void testWriteNoTransactionNoItems() throws Exception {
		writer.write(null);

		verifyZeroInteractions(template);
		verifyZeroInteractions(bulkOperations);
	}

	@Test
	public void testWriteTransactionNoCollection() throws Exception {
		final List<Object> items = new ArrayList<Object>() {{
			add(new Item("Foo"));
			add(new Item("Bar"));
		}};

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			try {
				writer.write(items);
			} catch (Exception e) {
				fail("An exception was thrown while writing: " + e.getMessage());
			}

			return null;
		});

		verify(template).bulkOps(any(), any(Class.class));
		verify(bulkOperations, times(2)).replaceOne(any(Query.class), any(Object.class), any());
	}

	@Test
	public void testWriteTransactionWithCollection() throws Exception {
		final List<Object> items = new ArrayList<Object>() {{
			add(new Item("Foo"));
			add(new Item("Bar"));
		}};

		writer.setCollection("collection");

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			try {
				writer.write(items);
			} catch (Exception e) {
				fail("An exception was thrown while writing: " + e.getMessage());
			}

			return null;
		});

		verify(template).bulkOps(any(), eq("collection"));
		verify(bulkOperations, times(2)).replaceOne(any(Query.class), any(Object.class), any());
	}

	@Test
	public void testWriteTransactionFails() throws Exception {
		final List<Object> items = new ArrayList<Object>() {{
			add(new Item("Foo"));
			add(new Item("Bar"));
		}};

		writer.setCollection("collection");

		try {
			new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
				try {
					writer.write(items);
				} catch (Exception ignore) {
					fail("unexpected exception thrown");
				}
				throw new RuntimeException("force rollback");
			});
		} catch (RuntimeException re) {
			assertEquals(re.getMessage(), "force rollback");
		} catch (Throwable t) {
			fail("Unexpected exception was thrown");
		}

		verifyZeroInteractions(template);
		verifyZeroInteractions(bulkOperations);
	}

	/**
	 * A pointless use case but validates that the flag is still honored.
	 *
	 */
	@Test
	public void testWriteTransactionReadOnly() throws Exception {
		final List<Object> items = new ArrayList<Object>() {{
			add(new Item("Foo"));
			add(new Item("Bar"));
		}};

		writer.setCollection("collection");

		try {
			TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
			transactionTemplate.setReadOnly(true);
			transactionTemplate.execute((TransactionCallback<Void>) status -> {
				try {
					writer.write(items);
				} catch (Exception ignore) {
					fail("unexpected exception thrown");
				}
				return null;
			});
		} catch (Throwable t) {
			fail("Unexpected exception was thrown");
		}

		verifyZeroInteractions(template);
		verifyZeroInteractions(bulkOperations);
	}

	@Test
	public void testRemoveNoObjectIdNoCollection() throws Exception {
		writer.setDelete(true);
		List<Object> items = new ArrayList<Object>() {{
			add(new Item("Foo"));
			add(new Item("Bar"));
		}};

		writer.write(items);

		verify(template).bulkOps(any(), any(Class.class));
		verify(bulkOperations, never()).remove(any(Query.class));
	}

	@Test
	public void testRemoveNoObjectIdWithCollection() throws Exception {
		writer.setDelete(true);
		List<Object> items = new ArrayList<Object>() {{
			add(new Item("Foo"));
			add(new Item("Bar"));
		}};

		writer.setCollection("collection");
		writer.write(items);

		verify(template).bulkOps(any(), eq("collection"));
		verify(bulkOperations, never()).remove(any(Query.class));
	}

	@Test
	public void testRemoveNoTransactionNoCollection() throws Exception {
		writer.setDelete(true);
		List<Object> items = new ArrayList<Object>() {{
			add(new Item(1));
			add(new Item(2));
		}};

		writer.write(items);

		verify(template).bulkOps(any(), any(Class.class));
		verify(bulkOperations, times(2)).remove(any(Query.class));
	}

	@Test
	public void testRemoveNoTransactionWithCollection() throws Exception {
		writer.setDelete(true);
		List<Object> items = new ArrayList<Object>() {{
			add(new Item(1));
			add(new Item(2));
		}};

		writer.setCollection("collection");

		writer.write(items);

		verify(template).bulkOps(any(),  eq("collection"));
		verify(bulkOperations, times(2)).remove(any(Query.class));
	}

	// BATCH-2018, test code updated to pass BATCH-3713
	@Test
	public void testResourceKeyCollision() throws Exception {
		final int limit = 5000;
		@SuppressWarnings("unchecked")
		List<MongoItemWriter<String>> writers = new ArrayList<>(limit);
		final String[] documents = new String[limit];
		final String[] results = new String[limit];
		for(int i = 0; i< limit; i++) {
			final int index = i;
			MongoOperations mongoOperations = mock(MongoOperations.class);
			BulkOperations bulkOperations = mock(BulkOperations.class);
			MongoConverter mongoConverter = mock(MongoConverter.class);

			when(mongoOperations.bulkOps(any(), any(Class.class))).thenReturn(bulkOperations);
			when(mongoOperations.getConverter()).thenReturn(mongoConverter);

			// mocking the object to document conversion which is used in forming bulk operation
			doAnswer(invocation -> {
				documents[index] = (String) invocation.getArguments()[0];
				return null;
			}).when(mongoConverter).write(any(String.class), any(Document.class));

			doAnswer(invocation -> {
				if(results[index] == null) {
					results[index] = documents[index];
				} else {
					results[index] += documents[index];
				}
				return null;
			}).when(bulkOperations).replaceOne(any(Query.class), any(Document.class), any());

			writers.add(i, new MongoItemWriter<>());
			writers.get(i).setTemplate(mongoOperations);
		}

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			try {
				for(int i=0; i< limit; i++) {
					writers.get(i).write(Collections.singletonList(String.valueOf(i)));
				}
			}
			catch (Exception e) {
				throw new IllegalStateException("Unexpected Exception", e);
			}
			return null;
		});

		for(int i=0; i< limit; i++) {
			assertEquals(String.valueOf(i), results[i]);
		}
	}

	static class Item {
		Integer id;
		String name;
		public Item(Integer id) {
			this.id = id;
		}
		public Item(String name) {
			this.name = name;
		}
	}
}