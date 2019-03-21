/*
 * Copyright 2006-2008 the original author or authors.
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

package org.springframework.batch.item.database.support;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.junit.Test;

import org.springframework.batch.item.database.orm.JpaNativeQueryProvider;
import org.springframework.batch.item.sample.Foo;
import org.springframework.util.Assert;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Anatoly Polinsky
 * @author Dave Syer
 * @author Will Schipp
 */
public class JpaNativeQueryProviderTests {

	private JpaNativeQueryProvider<Foo> jpaQueryProvider;

	public JpaNativeQueryProviderTests() {
		jpaQueryProvider = new JpaNativeQueryProvider<>();
		jpaQueryProvider.setEntityClass(Foo.class);
	}

	@Test
	public void testCreateQuery() {

		String sqlQuery = "select * from T_FOOS where value >= :limit";
		jpaQueryProvider.setSqlQuery(sqlQuery);

		EntityManager entityManager = mock(EntityManager.class);
		Query query = mock(Query.class);

		when(entityManager.createNativeQuery(sqlQuery, Foo.class)).thenReturn(query);

		jpaQueryProvider.setEntityManager(entityManager);
		Assert.notNull(jpaQueryProvider.createQuery(), "Query was null");
	}
}
