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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Dave Syer
 * @author Thomas Risberg
 * @author Michael Minella
 * @author Will Schipp
 * @author Mahmoud Ben Hassine
 */
public class HibernateItemWriterTests {

	HibernateItemWriter<Object> writer;

	SessionFactory factory;

	Session currentSession;

	@BeforeEach
	public void setUp() throws Exception {
		writer = new HibernateItemWriter<>();
		factory = mock(SessionFactory.class);
		currentSession = mock(Session.class);

		when(this.factory.getCurrentSession()).thenReturn(this.currentSession);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.HibernateItemWriter#afterPropertiesSet()}
	 * @throws Exception
	 */
	@Test
	public void testAfterPropertiesSet() throws Exception {
		writer = new HibernateItemWriter<>();
		try {
			writer.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalStateException e) {
			// expected
			assertTrue(e.getMessage().contains("SessionFactory"), "Wrong message for exception: " + e.getMessage());
		}
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.HibernateItemWriter#afterPropertiesSet()}
	 * @throws Exception
	 */
	@Test
	public void testAfterPropertiesSetWithDelegate() throws Exception {
		writer.setSessionFactory(this.factory);
		writer.afterPropertiesSet();
	}

	@Test
	public void testWriteAndFlushSunnyDayHibernate3() throws Exception {
		this.writer.setSessionFactory(this.factory);
		when(this.currentSession.contains("foo")).thenReturn(true);
		when(this.currentSession.contains("bar")).thenReturn(false);
		this.currentSession.saveOrUpdate("bar");
		this.currentSession.flush();
		this.currentSession.clear();

		List<String> items = Arrays.asList(new String[] { "foo", "bar" });
		writer.write(items);

	}

	@Test
	public void testWriteAndFlushWithFailureHibernate3() throws Exception {
		this.writer.setSessionFactory(this.factory);
		final RuntimeException ex = new RuntimeException("ERROR");
		when(this.currentSession.contains("foo")).thenThrow(ex);

		try {
			writer.write(Collections.singletonList("foo"));
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("ERROR", e.getMessage());
		}

	}

	@Test
	public void testWriteAndFlushSunnyDayHibernate4() throws Exception {
		writer.setSessionFactory(factory);
		when(factory.getCurrentSession()).thenReturn(currentSession);
		when(currentSession.contains("foo")).thenReturn(true);
		when(currentSession.contains("bar")).thenReturn(false);
		currentSession.saveOrUpdate("bar");
		currentSession.flush();
		currentSession.clear();

		List<String> items = Arrays.asList(new String[] { "foo", "bar" });
		writer.write(items);
	}

	@Test
	public void testWriteAndFlushWithFailureHibernate4() throws Exception {
		writer.setSessionFactory(factory);
		final RuntimeException ex = new RuntimeException("ERROR");

		when(factory.getCurrentSession()).thenReturn(currentSession);
		when(currentSession.contains("foo")).thenThrow(ex);

		try {
			writer.write(Collections.singletonList("foo"));
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("ERROR", e.getMessage());
		}
	}

}
