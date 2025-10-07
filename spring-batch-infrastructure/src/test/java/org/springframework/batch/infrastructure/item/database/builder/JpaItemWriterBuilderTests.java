/*
 * Copyright 2018-2023 the original author or authors.
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
package org.springframework.batch.infrastructure.item.database.builder;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.database.JpaItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JpaItemWriterBuilder;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Mahmoud Ben Hassine
 * @author Jinwoo Bae
 */
@ExtendWith(MockitoExtension.class)
class JpaItemWriterBuilderTests {

	@Mock
	private EntityManagerFactory entityManagerFactory;

	@Mock
	private EntityManager entityManager;

	@BeforeEach
	void setUp() {
		TransactionSynchronizationManager.bindResource(this.entityManagerFactory,
				new EntityManagerHolder(this.entityManager));
	}

	@AfterEach
	void tearDown() {
		TransactionSynchronizationManager.unbindResource(this.entityManagerFactory);
	}

	@Test
	void testConfiguration() throws Exception {
		JpaItemWriter<String> itemWriter = new JpaItemWriterBuilder<String>()
			.entityManagerFactory(this.entityManagerFactory)
			.build();

		Chunk<String> chunk = Chunk.of("foo", "bar");

		itemWriter.write(chunk);

		verify(this.entityManager).merge(chunk.getItems().get(0));
		verify(this.entityManager).merge(chunk.getItems().get(1));
		verify(this.entityManager).clear();
	}

	@Test
	void testValidation() {
		Exception exception = assertThrows(IllegalStateException.class,
				() -> new JpaItemWriterBuilder<String>().build());
		assertEquals("EntityManagerFactory must be provided", exception.getMessage());
	}

	@Test
	void testPersist() throws Exception {
		JpaItemWriter<String> itemWriter = new JpaItemWriterBuilder<String>()
			.entityManagerFactory(this.entityManagerFactory)
			.usePersist(true)
			.build();

		Chunk<String> chunk = Chunk.of("foo", "bar");

		itemWriter.write(chunk);

		verify(this.entityManager).persist(chunk.getItems().get(0));
		verify(this.entityManager).persist(chunk.getItems().get(1));
		verify(this.entityManager).clear();
	}

	@Test
	void testClearPersistenceContext() throws Exception {
		JpaItemWriter<String> itemWriter = new JpaItemWriterBuilder<String>().clearPersistenceContext(false)
			.entityManagerFactory(this.entityManagerFactory)
			.build();

		Chunk<String> chunk = Chunk.of("foo", "bar");

		itemWriter.write(chunk);

		verify(this.entityManager).merge(chunk.getItems().get(0));
		verify(this.entityManager).merge(chunk.getItems().get(1));
		verify(this.entityManager, never()).clear();
	}

}
