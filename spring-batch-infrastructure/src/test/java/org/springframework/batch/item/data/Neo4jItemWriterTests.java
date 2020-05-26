/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.batch.item.data;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class Neo4jItemWriterTests {

	private Neo4jItemWriter<String> writer;

	@Mock
	private SessionFactory sessionFactory;
	@Mock
	private Session session;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testAfterPropertiesSet() throws Exception{

		writer = new Neo4jItemWriter<>();

		try {
			writer.afterPropertiesSet();
			fail("SessionFactory was not set but exception was not thrown.");
		} catch (IllegalStateException iae) {
			assertEquals("A SessionFactory is required", iae.getMessage());
		} catch (Throwable t) {
			fail("Wrong exception was thrown.");
		}

		writer.setSessionFactory(this.sessionFactory);

		writer.afterPropertiesSet();

		writer = new Neo4jItemWriter<>();

		writer.setSessionFactory(this.sessionFactory);

		writer.afterPropertiesSet();
	}

	@Test
	public void testWriteNullSession() throws Exception {

		writer = new Neo4jItemWriter<>();

		writer.setSessionFactory(this.sessionFactory);
		writer.afterPropertiesSet();

		writer.write(null);

		verifyZeroInteractions(this.session);
	}

	@Test
	public void testWriteNullWithSession() throws Exception {
		writer = new Neo4jItemWriter<>();

		writer.setSessionFactory(this.sessionFactory);
		writer.afterPropertiesSet();

		when(this.sessionFactory.openSession()).thenReturn(this.session);
		writer.write(null);

		verifyZeroInteractions(this.session);
	}

	@Test
	public void testWriteNoItemsWithSession() throws Exception {
		writer = new Neo4jItemWriter<>();

		writer.setSessionFactory(this.sessionFactory);
		writer.afterPropertiesSet();

		when(this.sessionFactory.openSession()).thenReturn(this.session);
		writer.write(new ArrayList<>());

		verifyZeroInteractions(this.session);
	}

	@Test
	public void testWriteItemsWithSession() throws Exception {
		writer = new Neo4jItemWriter<>();

		writer.setSessionFactory(this.sessionFactory);
		writer.afterPropertiesSet();

		List<String> items = new ArrayList<>();
		items.add("foo");
		items.add("bar");

		when(this.sessionFactory.openSession()).thenReturn(this.session);
		writer.write(items);

		verify(this.session).save("foo");
		verify(this.session).save("bar");
	}

	@Test
	public void testDeleteItemsWithSession() throws Exception {
		writer = new Neo4jItemWriter<>();

		writer.setSessionFactory(this.sessionFactory);
		writer.afterPropertiesSet();

		List<String> items = new ArrayList<>();
		items.add("foo");
		items.add("bar");

		writer.setDelete(true);

		when(this.sessionFactory.openSession()).thenReturn(this.session);
		writer.write(items);

		verify(this.session).delete("foo");
		verify(this.session).delete("bar");
	}
}
