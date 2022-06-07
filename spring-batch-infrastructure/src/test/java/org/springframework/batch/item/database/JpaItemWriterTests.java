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

package org.springframework.batch.item.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Thomas Risberg
 * @author Will Schipp
 * @author Chris Cranford
 * @author Mahmoud Ben Hassine
 */
public class JpaItemWriterTests {

	EntityManagerFactory emf;

	JpaItemWriter<Object> writer;

	@BeforeEach
	public void setUp() throws Exception {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
		writer = new JpaItemWriter<>();
		emf = mock(EntityManagerFactory.class, "emf");
		writer.setEntityManagerFactory(emf);
	}

	@Test
	public void testAfterPropertiesSet() throws Exception {
		writer = new JpaItemWriter<>();
		try {
			writer.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
			assertTrue(e.getMessage().contains("EntityManagerFactory"),
					"Wrong message for exception: " + e.getMessage());
		}
	}

	@Test
	public void testWriteAndFlushSunnyDay() throws Exception {
		EntityManager em = mock(EntityManager.class, "em");
		em.contains("foo");
		em.contains("bar");
		em.merge("bar");
		em.flush();
		TransactionSynchronizationManager.bindResource(emf, new EntityManagerHolder(em));

		List<String> items = Arrays.asList(new String[] { "foo", "bar" });

		writer.write(items);

		TransactionSynchronizationManager.unbindResource(emf);
	}

	@Test
	public void testPersist() throws Exception {
		writer.setUsePersist(true);
		EntityManager em = mock(EntityManager.class, "em");
		TransactionSynchronizationManager.bindResource(emf, new EntityManagerHolder(em));
		List<String> items = Arrays.asList("persist1", "persist2");
		writer.write(items);
		verify(em).persist(items.get(0));
		verify(em).persist(items.get(1));
		TransactionSynchronizationManager.unbindResource(emf);
	}

	@Test
	public void testWriteAndFlushWithFailure() throws Exception {
		final RuntimeException ex = new RuntimeException("ERROR");
		EntityManager em = mock(EntityManager.class, "em");
		em.contains("foo");
		em.contains("bar");
		em.merge("bar");
		when(em).thenThrow(ex);
		TransactionSynchronizationManager.bindResource(emf, new EntityManagerHolder(em));
		List<String> items = Arrays.asList(new String[] { "foo", "bar" });

		try {
			writer.write(items);
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("ERROR", e.getMessage());
		}

		TransactionSynchronizationManager.unbindResource(emf);
	}

}
