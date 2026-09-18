/*
 * Copyright 2026-present the original author or authors.
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

import org.springframework.batch.infrastructure.item.ExecutionContext;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaPagingItemReaderTests {

	private EntityManagerFactory entityManagerFactory;

	private JpaPagingItemReader<Object> reader;

	@BeforeEach
	void setUp() {
		entityManagerFactory = mock();
		reader = new JpaPagingItemReader<>(entityManagerFactory);
		reader.setQueryString("select o from Object o");
	}

	@Test
	void closeShouldNotThrowWhenReaderWasNeverOpened() throws Exception {
		reader.afterPropertiesSet();

		assertDoesNotThrow(reader::close);
	}

	@Test
	void closeShouldCloseEntityManagerWhenReaderWasOpened() throws Exception {
		EntityManager entityManager = mock();
		when(entityManagerFactory.createEntityManager(anyMap())).thenReturn(entityManager);
		reader.afterPropertiesSet();

		reader.open(new ExecutionContext());
		reader.close();

		verify(entityManager).close();
	}

}
