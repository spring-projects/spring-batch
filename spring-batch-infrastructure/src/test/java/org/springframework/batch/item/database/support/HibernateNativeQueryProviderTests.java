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

import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.query.NativeQuery;
import org.junit.Test;

import org.springframework.batch.item.database.orm.HibernateNativeQueryProvider;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Anatoly Polinsky
 * @author Dave Syer
 * @author Will Schipp
 */
public class HibernateNativeQueryProviderTests {

	protected HibernateNativeQueryProvider<Foo> hibernateQueryProvider;

	public HibernateNativeQueryProviderTests() {
		hibernateQueryProvider = new HibernateNativeQueryProvider<>();
		hibernateQueryProvider.setEntityClass(Foo.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testCreateQueryWithStatelessSession() {
		String sqlQuery = "select * from T_FOOS";
		hibernateQueryProvider.setSqlQuery(sqlQuery);

		StatelessSession session = mock(StatelessSession.class);
		NativeQuery<Foo> query = mock(NativeQuery.class);

		when(session.createNativeQuery(sqlQuery)).thenReturn(query);
		when(query.addEntity(Foo.class)).thenReturn(query);

		hibernateQueryProvider.setStatelessSession(session);
		assertNotNull(hibernateQueryProvider.createQuery());

	}

	@Test
	@SuppressWarnings("unchecked")
	public void shouldCreateQueryWithStatefulSession() {
		String sqlQuery = "select * from T_FOOS";
		hibernateQueryProvider.setSqlQuery(sqlQuery);

		Session session = mock(Session.class);
		NativeQuery<Foo> query = mock(NativeQuery.class);

		when(session.createNativeQuery(sqlQuery)).thenReturn(query);
		when(query.addEntity(Foo.class)).thenReturn(query);

		hibernateQueryProvider.setSession(session);
		assertNotNull(hibernateQueryProvider.createQuery());

	}

	private static class Foo {
	}

}
