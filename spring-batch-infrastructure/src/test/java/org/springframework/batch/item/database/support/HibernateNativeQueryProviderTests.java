/*
 * Copyright 2006-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.item.database.support;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import org.hibernate.SQLQuery;
import org.hibernate.StatelessSession;
import org.hibernate.classic.Session;
import org.junit.Test;
import org.springframework.batch.item.database.orm.HibernateNativeQueryProvider;
import org.springframework.util.Assert;

/**
 * @author Anatoly Polinsky
 * @author Dave Syer
 */
public class HibernateNativeQueryProviderTests {

	protected HibernateNativeQueryProvider<Foo> hibernateQueryProvider;

	public HibernateNativeQueryProviderTests() {
		hibernateQueryProvider = new HibernateNativeQueryProvider<Foo>();
		hibernateQueryProvider.setEntityClass(Foo.class);
	}

	@Test
	public void testCreateQueryWithStatelessSession() {
		String sqlQuery = "select * from T_FOOS";
		hibernateQueryProvider.setSqlQuery(sqlQuery);

		StatelessSession session = createMock(StatelessSession.class);
		SQLQuery query = createMock(SQLQuery.class);

		expect(session.createSQLQuery(sqlQuery)).andReturn(query);
		expect(query.addEntity(Foo.class)).andReturn(query);

		replay(session, query);

		hibernateQueryProvider.setStatelessSession(session);
		Assert.notNull(hibernateQueryProvider.createQuery());

		verify(session, query);
	}

	@Test
	public void shouldCreateQueryWithStatefulSession() {
		String sqlQuery = "select * from T_FOOS";
		hibernateQueryProvider.setSqlQuery(sqlQuery);

		Session session = createMock(Session.class);
		SQLQuery query = createMock(SQLQuery.class);

		expect(session.createSQLQuery(sqlQuery)).andReturn(query);
		expect(query.addEntity(Foo.class)).andReturn(query);

		replay(session, query);

		hibernateQueryProvider.setSession(session);
		Assert.notNull(hibernateQueryProvider.createQuery());

		verify(session, query);
	}
	
	private static class Foo {
	}

}
