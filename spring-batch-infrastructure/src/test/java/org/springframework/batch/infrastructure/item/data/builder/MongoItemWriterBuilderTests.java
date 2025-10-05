/*
 * Copyright 2017-2025 the original author or authors.
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

package org.springframework.batch.infrastructure.item.data.builder;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.data.MongoItemWriter;
import org.springframework.batch.infrastructure.item.data.builder.MongoItemWriterBuilder;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 * @author Parikshit Dutta
 */
@MockitoSettings(strictness = Strictness.LENIENT)
class MongoItemWriterBuilderTests {

	@Mock
	private MongoOperations template;

	@Mock
	private BulkOperations bulkOperations;

	@Mock
	DbRefResolver dbRefResolver;

	private MongoConverter mongoConverter;

	private Chunk<Item> saveItems;

	private Chunk<Item> removeItems;

	@BeforeEach
	void setUp() {
		when(this.template.bulkOps(any(), anyString())).thenReturn(this.bulkOperations);
		when(this.template.bulkOps(any(), any(Class.class))).thenReturn(this.bulkOperations);

		MappingContext<MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext = new MongoMappingContext();
		mongoConverter = spy(new MappingMongoConverter(this.dbRefResolver, mappingContext));
		when(this.template.getConverter()).thenReturn(mongoConverter);

		this.saveItems = Chunk.of(new Item("Foo"), new Item("Bar"));
		this.removeItems = Chunk.of(new Item(1), new Item(2));
	}

	@Test
	void testBasicWrite() throws Exception {
		MongoItemWriter<Item> writer = new MongoItemWriterBuilder<Item>().template(this.template).build();
		writer.write(this.saveItems);

		verify(this.template).bulkOps(any(), any(Class.class));
		verify(this.mongoConverter).write(eq(this.saveItems.getItems().get(0)), any(Document.class));
		verify(this.mongoConverter).write(eq(this.saveItems.getItems().get(1)), any(Document.class));
		verify(this.bulkOperations, times(2)).replaceOne(any(Query.class), any(Object.class), any());
		verify(this.bulkOperations, never()).remove(any(Query.class));
	}

	@Test
	void testWriteToCollection() throws Exception {
		MongoItemWriter<Item> writer = new MongoItemWriterBuilder<Item>().collection("collection")
			.template(this.template)
			.build();

		writer.write(this.saveItems);

		verify(this.template).bulkOps(any(), eq("collection"));
		verify(this.mongoConverter).write(eq(this.saveItems.getItems().get(0)), any(Document.class));
		verify(this.mongoConverter).write(eq(this.saveItems.getItems().get(1)), any(Document.class));
		verify(this.bulkOperations, times(2)).replaceOne(any(Query.class), any(Object.class), any());
		verify(this.bulkOperations, never()).remove(any(Query.class));
	}

	@Test
	void testDelete() throws Exception {
		MongoItemWriter<Item> writer = new MongoItemWriterBuilder<Item>().template(this.template)
			.mode(MongoItemWriter.Mode.REMOVE)
			.build();

		writer.write(this.removeItems);

		verify(this.template).bulkOps(any(), any(Class.class));
		verify(this.bulkOperations, times(2)).remove(any(Query.class));
	}

	@Test
	void testNullTemplate() {
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> new MongoItemWriterBuilder<>().build());
		assertEquals("template is required.", exception.getMessage());
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
