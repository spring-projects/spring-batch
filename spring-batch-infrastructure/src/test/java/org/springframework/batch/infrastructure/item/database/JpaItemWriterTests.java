/*
 * Copyright 2006-2022 the original author or authors.
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

package org.springframework.batch.infrastructure.item.database;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.database.JpaItemWriter;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Thomas Risberg
 * @author Will Schipp
 * @author Chris Cranford
 * @author Mahmoud Ben Hassine
 */
class JpaItemWriterTests {

	EntityManagerFactory emf;

	JpaItemWriter<Object> writer;

	@BeforeEach
	void setUp() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
		emf = mock();
		writer = new JpaItemWriter<>(emf);
	}

	@Test
	void testWriteAndFlushSunnyDay() {
		EntityManager em = mock();
		em.contains("foo");
		em.contains("bar");
		em.merge("bar");
		em.flush();
		TransactionSynchronizationManager.bindResource(emf, new EntityManagerHolder(em));

		Chunk<String> items = Chunk.of("foo", "bar");

		writer.write(items);

		TransactionSynchronizationManager.unbindResource(emf);
	}

	@Test
	void testPersist() {
		writer.setUsePersist(true);
		EntityManager em = mock();
		TransactionSynchronizationManager.bindResource(emf, new EntityManagerHolder(em));
		Chunk<String> chunk = Chunk.of("persist1", "persist2");
		writer.write(chunk);
		verify(em).persist(chunk.getItems().get(0));
		verify(em).persist(chunk.getItems().get(1));
		TransactionSynchronizationManager.unbindResource(emf);
	}

	@Test
	void testWriteAndFlushWithFailure() {
		final RuntimeException ex = new RuntimeException("ERROR");
		EntityManager em = mock();
		em.contains("foo");
		em.contains("bar");
		em.merge("bar");
		when(em).thenThrow(ex);
		TransactionSynchronizationManager.bindResource(emf, new EntityManagerHolder(em));

		Exception exception = assertThrows(RuntimeException.class, () -> writer.write(Chunk.of("foo", "bar")));
		assertEquals("ERROR", exception.getMessage());

		TransactionSynchronizationManager.unbindResource(emf);
	}

}
