/*
 * Copyright 2020-2022 the original author or authors.
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
package org.springframework.batch.item.database.orm;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.batch.item.sample.Foo;
import org.springframework.util.Assert;

/**
 * Test for {@link JpaNamedQueryProvider}s.
 *
 * @author Parikshit Dutta
 * @author Mahmoud Ben Hassine
 */
class JpaNamedQueryProviderTests {

	@Test
	void testJpaNamedQueryProviderNamedQueryIsProvided() {
		JpaNamedQueryProvider<Foo> jpaNamedQueryProvider = new JpaNamedQueryProvider<>();
		jpaNamedQueryProvider.setEntityClass(Foo.class);

		try {
			jpaNamedQueryProvider.afterPropertiesSet();
		}
		catch (Exception exception) {
			assertEquals("Named query cannot be empty", exception.getMessage());
		}
	}

	@Test
	void testJpaNamedQueryProviderEntityClassIsProvided() {
		JpaNamedQueryProvider<Foo> jpaNamedQueryProvider = new JpaNamedQueryProvider<>();
		jpaNamedQueryProvider.setNamedQuery("allFoos");

		try {
			jpaNamedQueryProvider.afterPropertiesSet();
		}
		catch (Exception exception) {
			assertEquals("Entity class cannot be NULL", exception.getMessage());
		}
	}

	@Test
	void testNamedQueryCreation() throws Exception {
		// given
		String namedQuery = "allFoos";
		TypedQuery<Foo> query = mock();
		EntityManager entityManager = Mockito.mock();
		when(entityManager.createNamedQuery(namedQuery, Foo.class)).thenReturn(query);
		JpaNamedQueryProvider<Foo> jpaNamedQueryProvider = new JpaNamedQueryProvider<>();
		jpaNamedQueryProvider.setEntityManager(entityManager);
		jpaNamedQueryProvider.setEntityClass(Foo.class);
		jpaNamedQueryProvider.setNamedQuery(namedQuery);
		jpaNamedQueryProvider.afterPropertiesSet();

		// when
		Query result = jpaNamedQueryProvider.createQuery();

		// then
		assertNotNull(result);
		verify(entityManager).createNamedQuery(namedQuery, Foo.class);
	}

}
