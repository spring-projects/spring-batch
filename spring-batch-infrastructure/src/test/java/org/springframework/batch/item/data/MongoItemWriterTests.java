/*
 * Copyright 2013-2025 the original author or authors.
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
import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.data.MongoItemWriter.Mode;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Michael Minella
 * @author Parikshit Dutta
 * @author Mahmoud Ben Hassine
 */
@MockitoSettings(strictness = Strictness.LENIENT)
class MongoItemWriterTests {

	private MongoItemWriter<Object> writer;

	@Mock
	private MongoOperations template;

	@Mock
	private BulkOperations bulkOperations;

	@Mock
	DbRefResolver dbRefResolver;

	private final PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

	@BeforeEach
	void setUp() throws Exception {
		when(this.template.bulkOps(any(), anyString())).thenReturn(this.bulkOperations);
		when(this.template.bulkOps(any(), any(Class.class))).thenReturn(this.bulkOperations);

		MappingContext<MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext = new MongoMappingContext();
		MappingMongoConverter mongoConverter = spy(new MappingMongoConverter(this.dbRefResolver, mappingContext));
		when(this.template.getConverter()).thenReturn(mongoConverter);

		writer = new MongoItemWriter<>();
		writer.setTemplate(template);
		writer.afterPropertiesSet();
	}

	@Test
	void testAfterPropertiesSet() throws Exception {
		writer = new MongoItemWriter<>();
		assertThrows(IllegalStateException.class, writer::afterPropertiesSet);

		writer.setTemplate(template);
		writer.afterPropertiesSet();
	}

	@Test
	void testWriteNoTransactionNoCollection() throws Exception {
		Chunk<Item> items = Chunk.of(new Item("Foo"), new Item("Bar"));

		writer.write(items);

		verify(template).bulkOps(any(), any(Class.class));
		verify(bulkOperations, times(2)).replaceOne(any(Query.class), any(Object.class), any());
	}

	@Test
	void testWriteNoTransactionWithCollection() throws Exception {
		Chunk<Object> items = Chunk.of(new Item("Foo"), new Item("Bar"));

		writer.setCollection("collection");

		writer.write(items);

		verify(template).bulkOps(any(), eq("collection"));
		verify(bulkOperations, times(2)).replaceOne(any(Query.class), any(Object.class), any());
	}

	@Test
	void testWriteNoTransactionNoItems() throws Exception {
		writer.write(new Chunk<>());

		verifyNoInteractions(template);
		verifyNoInteractions(bulkOperations);
	}

	@Test
	void testWriteTransactionNoCollection() {
		final Chunk<Object> items = Chunk.of(new Item("Foo"), new Item("Bar"));

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			assertDoesNotThrow(() -> writer.write(items));
			return null;
		});

		verify(template).bulkOps(any(), any(Class.class));
		verify(bulkOperations, times(2)).replaceOne(any(Query.class), any(Object.class), any());
	}

	@Test
	void testWriteTransactionWithCollection() {
		final Chunk<Object> items = Chunk.of(new Item("Foo"), new Item("Bar"));

		writer.setCollection("collection");

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			assertDoesNotThrow(() -> writer.write(items));
			return null;
		});

		verify(template).bulkOps(any(), eq("collection"));
		verify(bulkOperations, times(2)).replaceOne(any(Query.class), any(Object.class), any());
	}

	@Test
	void testWriteTransactionFails() {
		final Chunk<Object> items = Chunk.of(new Item("Foo"), new Item("Bar"));

		writer.setCollection("collection");

		Exception exception = assertThrows(RuntimeException.class,
				() -> new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
					assertDoesNotThrow(() -> writer.write(items));
					throw new RuntimeException("force rollback");
				}));
		assertEquals(exception.getMessage(), "force rollback");

		verifyNoInteractions(template);
		verifyNoInteractions(bulkOperations);
	}

	/**
	 * A pointless use case but validates that the flag is still honored.
	 *
	 */
	@Test
	void testWriteTransactionReadOnly() {
		final Chunk<Object> items = Chunk.of(new Item("Foo"), new Item("Bar"));

		writer.setCollection("collection");

		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.setReadOnly(true);
		transactionTemplate.execute((TransactionCallback<Void>) status -> {
			assertDoesNotThrow(() -> writer.write(items));
			return null;
		});

		verifyNoInteractions(template);
		verifyNoInteractions(bulkOperations);
	}

	@Test
	void testRemoveNoObjectIdNoCollection() throws Exception {
		writer.setMode(Mode.REMOVE);
		Chunk<Object> items = Chunk.of(new Item("Foo"), new Item("Bar"));

		writer.write(items);

		verify(template).bulkOps(any(), any(Class.class));
		verify(bulkOperations, never()).remove(any(Query.class));
	}

	@Test
	void testRemoveNoObjectIdWithCollection() throws Exception {
		writer.setMode(Mode.REMOVE);
		Chunk<Object> items = Chunk.of(new Item("Foo"), new Item("Bar"));

		writer.setCollection("collection");
		writer.write(items);

		verify(template).bulkOps(any(), eq("collection"));
		verify(bulkOperations, never()).remove(any(Query.class));
	}

	@Test
	void testRemoveNoTransactionNoCollection() throws Exception {
		writer.setMode(Mode.REMOVE);
		Chunk<Object> items = Chunk.of(new Item(1), new Item(2));

		writer.write(items);

		verify(template).bulkOps(any(), any(Class.class));
		verify(bulkOperations, times(2)).remove(any(Query.class));
	}

	@Test
	void testRemoveNoTransactionWithCollection() throws Exception {
		writer.setMode(Mode.REMOVE);
		Chunk<Object> items = Chunk.of(new Item(1), new Item(2));

		writer.setCollection("collection");

		writer.write(items);

		verify(template).bulkOps(any(), eq("collection"));
		verify(bulkOperations, times(2)).remove(any(Query.class));
	}

	// BATCH-2018, test code updated to pass BATCH-3713
	@Test
	void testResourceKeyCollision() {
		final int limit = 5000;
		List<MongoItemWriter<String>> writers = new ArrayList<>(limit);
		final String[] documents = new String[limit];
		final String[] results = new String[limit];
		for (int i = 0; i < limit; i++) {
			final int index = i;
			MongoOperations mongoOperations = mock();
			BulkOperations bulkOperations = mock();
			MongoConverter mongoConverter = mock();

			when(mongoOperations.bulkOps(any(), any(Class.class))).thenReturn(bulkOperations);
			when(mongoOperations.getConverter()).thenReturn(mongoConverter);

			// mocking the object to document conversion which is used in forming bulk
			// operation
			doAnswer(invocation -> {
				documents[index] = (String) invocation.getArguments()[0];
				return null;
			}).when(mongoConverter).write(any(String.class), any(Document.class));

			doAnswer(invocation -> {
				if (results[index] == null) {
					results[index] = documents[index];
				}
				else {
					results[index] += documents[index];
				}
				return null;
			}).when(bulkOperations).replaceOne(any(Query.class), any(Document.class), any());

			writers.add(i, new MongoItemWriter<>());
			writers.get(i).setTemplate(mongoOperations);
		}

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			try {
				for (int i = 0; i < limit; i++) {
					writers.get(i).write(Chunk.of(String.valueOf(i)));
				}
			}
			catch (Exception e) {
				throw new IllegalStateException("Unexpected Exception", e);
			}
			return null;
		});

		for (int i = 0; i < limit; i++) {
			assertEquals(String.valueOf(i), results[i]);
		}
	}

	// BATCH-4149

	@Test
	void testInsertModeNoTransactionNoCollection() throws Exception {
		Chunk<Item> items = Chunk.of(new Item("Foo"), new Item("Bar"));

		writer.setMode(Mode.INSERT);
		writer.write(items);

		verify(template).bulkOps(any(), any(Class.class));
		verify(bulkOperations, times(2)).insert(any(Object.class));
	}

	@Test
	void testInsertModeNoTransactionWithCollection() throws Exception {
		Chunk<Object> items = Chunk.of(new Item("Foo"), new Item("Bar"));

		writer.setMode(Mode.INSERT);
		writer.setCollection("collection");

		writer.write(items);

		verify(template).bulkOps(any(), eq("collection"));
		verify(bulkOperations, times(2)).insert(any(Object.class));
	}

	@Test
	void testInsertModeNoTransactionNoItems() throws Exception {
		writer.setMode(Mode.INSERT);
		writer.write(new Chunk<>());

		verifyNoInteractions(template);
		verifyNoInteractions(bulkOperations);
	}

	@Test
	void testInsertModeTransactionNoCollection() {
		final Chunk<Object> items = Chunk.of(new Item("Foo"), new Item("Bar"));

		writer.setMode(Mode.INSERT);

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			assertDoesNotThrow(() -> writer.write(items));
			return null;
		});

		verify(template).bulkOps(any(), any(Class.class));
		verify(bulkOperations, times(2)).insert(any(Object.class));
	}

	@Test
	void testInsertModeTransactionWithCollection() {
		final Chunk<Object> items = Chunk.of(new Item("Foo"), new Item("Bar"));

		writer.setMode(Mode.INSERT);
		writer.setCollection("collection");

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			assertDoesNotThrow(() -> writer.write(items));
			return null;
		});

		verify(template).bulkOps(any(), eq("collection"));
		verify(bulkOperations, times(2)).insert(any(Object.class));
	}

	@Test
	void testInsertModeTransactionFails() {
		final Chunk<Object> items = Chunk.of(new Item("Foo"), new Item("Bar"));

		writer.setMode(Mode.INSERT);
		writer.setCollection("collection");

		Exception exception = assertThrows(RuntimeException.class,
				() -> new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
					assertDoesNotThrow(() -> writer.write(items));
					throw new RuntimeException("force rollback");
				}));
		assertEquals(exception.getMessage(), "force rollback");

		verifyNoInteractions(template);
		verifyNoInteractions(bulkOperations);
	}

	@Test
	void testInsertModeTransactionReadOnly() {
		final Chunk<Object> items = Chunk.of(new Item("Foo"), new Item("Bar"));

		writer.setMode(Mode.INSERT);
		writer.setCollection("collection");

		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.setReadOnly(true);
		transactionTemplate.execute((TransactionCallback<Void>) status -> {
			assertDoesNotThrow(() -> writer.write(items));
			return null;
		});

		verifyNoInteractions(template);
		verifyNoInteractions(bulkOperations);
	}

	@Test
	void testRemoveModeNoObjectIdNoCollection() throws Exception {
		writer.setMode(Mode.REMOVE);
		Chunk<Object> items = Chunk.of(new Item("Foo"), new Item("Bar"));

		writer.write(items);

		verify(template).bulkOps(any(), any(Class.class));
		verify(bulkOperations, never()).remove(any(Query.class));
	}

	@Test
	void testRemoveModeNoObjectIdWithCollection() throws Exception {
		writer.setMode(Mode.REMOVE);
		Chunk<Object> items = Chunk.of(new Item("Foo"), new Item("Bar"));

		writer.setCollection("collection");
		writer.write(items);

		verify(template).bulkOps(any(), eq("collection"));
		verify(bulkOperations, never()).remove(any(Query.class));
	}

	@Test
	void testRemoveModeNoTransactionNoCollection() throws Exception {
		writer.setMode(Mode.REMOVE);
		Chunk<Object> items = Chunk.of(new Item(1), new Item(2));

		writer.write(items);

		verify(template).bulkOps(any(), any(Class.class));
		verify(bulkOperations, times(2)).remove(any(Query.class));
	}

	@Test
	void testRemoveModeNoTransactionWithCollection() throws Exception {
		writer.setMode(Mode.REMOVE);
		Chunk<Object> items = Chunk.of(new Item(1), new Item(2));

		writer.setCollection("collection");

		writer.write(items);

		verify(template).bulkOps(any(), eq("collection"));
		verify(bulkOperations, times(2)).remove(any(Query.class));
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