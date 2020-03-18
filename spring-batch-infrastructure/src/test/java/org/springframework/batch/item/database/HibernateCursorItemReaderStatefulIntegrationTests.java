/*
 * Copyright 2008-2013 the original author or authors.
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

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.junit.Test;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.sample.Foo;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link HibernateCursorItemReader} using standard hibernate {@link Session}.
 *
 * @author Robert Kasanicky
 * @author Will Schipp
 */
public class HibernateCursorItemReaderStatefulIntegrationTests extends AbstractHibernateCursorItemReaderIntegrationTests {

	@Override
	protected boolean isUseStatelessSession() {
		return false;
	}

	//Ensure close is called on the stateful session correctly.
	@Test
	@SuppressWarnings("unchecked")
	public void testStatefulClose(){

		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		Query<Foo> scrollableResults = mock(Query.class);
		HibernateCursorItemReader<Foo> itemReader = new HibernateCursorItemReader<>();
		itemReader.setSessionFactory(sessionFactory);
		itemReader.setQueryString("testQuery");
		itemReader.setUseStatelessSession(false);

		when(sessionFactory.openSession()).thenReturn(session);
		when(session.createQuery("testQuery")).thenReturn(scrollableResults);
		when(scrollableResults.setFetchSize(0)).thenReturn(scrollableResults);

		itemReader.open(new ExecutionContext());
		itemReader.close();
	}

}
