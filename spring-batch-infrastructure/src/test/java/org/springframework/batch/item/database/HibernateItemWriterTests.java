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

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.item.Chunk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Dave Syer
 * @author Thomas Risberg
 * @author Michael Minella
 * @author Will Schipp
 * @author Mahmoud Ben Hassine
 */
class HibernateItemWriterTests {

	HibernateItemWriter<Object> writer;

	SessionFactory factory;

	Session currentSession;

	@BeforeEach
	void setUp() {
		writer = new HibernateItemWriter<>();
		factory = mock(SessionFactory.class);
		currentSession = mock(Session.class);

		when(this.factory.getCurrentSession()).thenReturn(this.currentSession);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.HibernateItemWriter#afterPropertiesSet()}
	 */
	@Test
	void testAfterPropertiesSet() {
		writer = new HibernateItemWriter<>();
		Exception exception = assertThrows(IllegalStateException.class, writer::afterPropertiesSet);
		String message = exception.getMessage();
		assertTrue(message.contains("SessionFactory"), "Wrong message for exception: " + message);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.HibernateItemWriter#afterPropertiesSet()}
	 */
	@Test
	void testAfterPropertiesSetWithDelegate() {
		writer.setSessionFactory(this.factory);
		writer.afterPropertiesSet();
	}

	@Test
	void testWriteAndFlushSunnyDayHibernate3() {
		this.writer.setSessionFactory(this.factory);
		when(this.currentSession.contains("foo")).thenReturn(true);
		when(this.currentSession.contains("bar")).thenReturn(false);
		this.currentSession.saveOrUpdate("bar");
		this.currentSession.flush();
		this.currentSession.clear();

		Chunk<String> items = Chunk.of("foo", "bar");
		writer.write(items);

	}

	@Test
	void testWriteAndFlushWithFailureHibernate3() {
		this.writer.setSessionFactory(this.factory);
		final RuntimeException ex = new RuntimeException("ERROR");
		when(this.currentSession.contains("foo")).thenThrow(ex);

		Exception exception = assertThrows(RuntimeException.class, () -> writer.write(Chunk.of("foo")));
		assertEquals("ERROR", exception.getMessage());
	}

	@Test
	void testWriteAndFlushSunnyDayHibernate4() {
		writer.setSessionFactory(factory);
		when(factory.getCurrentSession()).thenReturn(currentSession);
		when(currentSession.contains("foo")).thenReturn(true);
		when(currentSession.contains("bar")).thenReturn(false);
		currentSession.saveOrUpdate("bar");
		currentSession.flush();
		currentSession.clear();

		Chunk<String> items = Chunk.of("foo", "bar");
		writer.write(items);
	}

	@Test
	void testWriteAndFlushWithFailureHibernate4() {
		writer.setSessionFactory(factory);
		final RuntimeException ex = new RuntimeException("ERROR");

		when(factory.getCurrentSession()).thenReturn(currentSession);
		when(currentSession.contains("foo")).thenThrow(ex);

		Exception exception = assertThrows(RuntimeException.class, () -> writer.write(Chunk.of("foo")));
		assertEquals("ERROR", exception.getMessage());
	}

}
