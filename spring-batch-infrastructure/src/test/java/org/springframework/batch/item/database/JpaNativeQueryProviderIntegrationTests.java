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

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;

import org.junit.jupiter.api.Test;
import org.springframework.batch.item.database.orm.JpaNativeQueryProvider;
import org.springframework.batch.item.sample.Foo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Anatoly Polinsky
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 */
@SpringJUnitConfig(locations = { "JpaPagingItemReaderCommonTests-context.xml" })
public class JpaNativeQueryProviderIntegrationTests {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	private final JpaNativeQueryProvider<Foo> jpaQueryProvider;

	public JpaNativeQueryProviderIntegrationTests() {
		jpaQueryProvider = new JpaNativeQueryProvider<>();
		jpaQueryProvider.setEntityClass(Foo.class);
	}

	@Test
	@Transactional
	void shouldRetrieveAndMapAllFoos() throws Exception {

		String sqlQuery = "select * from T_FOOS";
		jpaQueryProvider.setSqlQuery(sqlQuery);
		jpaQueryProvider.afterPropertiesSet();
		jpaQueryProvider.setEntityManager(entityManagerFactory.createEntityManager());

		Query query = jpaQueryProvider.createQuery();

		List<Foo> expectedFoos = new ArrayList<>();

		expectedFoos.add(new Foo(1, "bar1", 1));
		expectedFoos.add(new Foo(2, "bar2", 2));
		expectedFoos.add(new Foo(3, "bar3", 3));
		expectedFoos.add(new Foo(4, "bar4", 4));
		expectedFoos.add(new Foo(5, "bar5", 5));

		@SuppressWarnings("unchecked")
		List<Foo> actualFoos = query.getResultList();

		assertEquals(actualFoos, expectedFoos);
	}

	@Test
	@Transactional
	void shouldExecuteParameterizedQuery() throws Exception {

		String sqlQuery = "select * from T_FOOS where value >= :limit";

		jpaQueryProvider.setSqlQuery(sqlQuery);
		jpaQueryProvider.afterPropertiesSet();
		jpaQueryProvider.setEntityManager(entityManagerFactory.createEntityManager());

		Query query = jpaQueryProvider.createQuery();
		query.setParameter("limit", 3);

		List<Foo> expectedFoos = new ArrayList<>();

		expectedFoos.add(new Foo(3, "bar3", 3));
		expectedFoos.add(new Foo(4, "bar4", 4));
		expectedFoos.add(new Foo(5, "bar5", 5));

		@SuppressWarnings("unchecked")
		List<Foo> actualFoos = query.getResultList();

		assertEquals(actualFoos, expectedFoos);
	}

}
