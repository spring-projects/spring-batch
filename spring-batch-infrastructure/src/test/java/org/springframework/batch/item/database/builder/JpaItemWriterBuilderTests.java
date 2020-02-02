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
package org.springframework.batch.item.database.builder;

import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;

/**
 * @author Mahmoud Ben Hassine
 */
public class JpaItemWriterBuilderTests {

	@Mock
	private EntityManagerFactory entityManagerFactory;

	@Mock
	private EntityManager entityManager;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		TransactionSynchronizationManager.bindResource(this.entityManagerFactory,
				new EntityManagerHolder(this.entityManager));
	}

	@After
	public void tearDown() {
		TransactionSynchronizationManager.unbindResource(this.entityManagerFactory);
	}

	@Test
	public void testConfiguration() throws Exception {
		JpaItemWriter<String> itemWriter = new JpaItemWriterBuilder<String>()
				.entityManagerFactory(this.entityManagerFactory)
				.build();

		itemWriter.afterPropertiesSet();

		List<String> items = Arrays.asList("foo", "bar");

		itemWriter.write(items);

		verify(this.entityManager).merge(items.get(0));
		verify(this.entityManager).merge(items.get(1));
	}

	@Test
	public void testValidation() {
		try {
			new JpaItemWriterBuilder<String>()
					.build();
			fail("Should fail if no EntityManagerFactory is provided");
		}
		catch (IllegalStateException ise) {
			assertEquals("Incorrect message", "EntityManagerFactory must be provided", ise.getMessage());
		}
	}

	@Test
	public void testPersist() throws Exception {
		JpaItemWriter<String> itemWriter = new JpaItemWriterBuilder<String>()
				.entityManagerFactory(this.entityManagerFactory)
				.usePersist(true)
				.build();

		itemWriter.afterPropertiesSet();

		List<String> items = Arrays.asList("foo", "bar");

		itemWriter.write(items);

		verify(this.entityManager).persist(items.get(0));
		verify(this.entityManager).persist(items.get(1));
	}

}
