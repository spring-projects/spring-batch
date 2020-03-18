/*
 * Copyright 2006-2009 the original author or authors.
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;


/**
 * @author Dave Syer
 * @author Will Schipp
 *
 */
public class HibernateItemReaderHelperTests {
	
	private HibernateItemReaderHelper<String> helper = new HibernateItemReaderHelper<>();
	
	private SessionFactory sessionFactory = mock(SessionFactory.class);
	
	@Test
	public void testOneSessionForAllPages() throws Exception {

		StatelessSession session = mock(StatelessSession.class);
		when(sessionFactory.openStatelessSession()).thenReturn(session);
		
		helper.setSessionFactory(sessionFactory);

		helper.createQuery();
		// Multiple calls to createQuery only creates one session
		helper.createQuery();

	}

	@Test
	public void testSessionReset() throws Exception {

		StatelessSession session = mock(StatelessSession.class);
		when(sessionFactory.openStatelessSession()).thenReturn(session);
		
		helper.setSessionFactory(sessionFactory);

		helper.createQuery();
		assertNotNull(ReflectionTestUtils.getField(helper, "statelessSession"));

		helper.close();		
		assertNull(ReflectionTestUtils.getField(helper, "statelessSession"));

	}

}
