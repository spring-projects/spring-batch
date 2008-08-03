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

package org.springframework.batch.item.database;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Thomas Risberg
 *
 */
public class JpaAwareItemWriterTests {

	JpaAwareItemWriter<Object> writer = new JpaAwareItemWriter<Object>();
	
	ItemWriter<Object> delegate;

	EntityManagerFactory emf;

	final List<Object> list = new ArrayList<Object>();

	@Before
	@SuppressWarnings({"unchecked"})
	public void setUp() throws Exception {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();					
		}
		delegate = createMock("delegate", ItemWriter.class);
		writer.setDelegate(delegate);
		emf = createMock("emf", EntityManagerFactory.class);
		writer.setEntityManagerFactory(emf);
	}

	@Test
	public void testAfterPropertiesSet() throws Exception {
		writer = new JpaAwareItemWriter<Object>();
		try {
			writer.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// expected
			assertTrue("Wrong message for exception: " + e.getMessage(), e
					.getMessage().indexOf("delegate") >= 0);
		}
		writer.setDelegate(delegate);
		try {
			writer.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// expected
			assertTrue("Wrong message for exception: " + e.getMessage(), e
					.getMessage().indexOf("EntityManagerFactory") >= 0);
		}
	}

	@Test
	public void testWrite() throws Exception {
		delegate.write("foo");
		replay(delegate);
		writer.write("foo");
		verify(delegate);
	}

	@Test
	public void testFlushWithFailure() throws Exception{
		final RuntimeException ex = new RuntimeException("bar");
		EntityManager em = createMock("em", EntityManager.class);
		em.joinTransaction();
		em.flush();
		expectLastCall().andThrow(ex);
		replay(em);
		expect(emf.createEntityManager()).andReturn(em);
		replay(emf);
		TransactionSynchronizationManager.bindResource(emf, new EntityManagerHolder(em));
		delegate.flush();
		replay(delegate);
		try {
			writer.flush();
			fail("Expected RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("bar", e.getMessage());
		}
		TransactionSynchronizationManager.unbindResource(emf);
	}

	@Test
	public void testWriteAndFlushWithFailure() throws Exception {
		final RuntimeException ex = new RuntimeException("bar");
		EntityManager em = createMock("em", EntityManager.class);
		em.flush();
		expectLastCall().andThrow(ex);
		em.flush();
		em.clear();
		replay(em);
		replay(emf);
		TransactionSynchronizationManager.bindResource(emf, new EntityManagerHolder(em));
		delegate.write("foo");
		delegate.flush();
		delegate.write("spam");
		delegate.flush();
		replay(delegate);

		writer.write("foo");
		try {
			writer.flush();
			fail("Expected RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("bar", e.getMessage());
		}

		writer.write("spam");
		writer.flush();

		verify(delegate);
		verify(em);
		TransactionSynchronizationManager.unbindResource(emf);
	}

	@Test
	public void testFlush() throws Exception{
		EntityManager em = createMock("em", EntityManager.class);
		em.flush();
		em.clear();
		replay(em);
		replay(emf);
		TransactionSynchronizationManager.bindResource(emf, new EntityManagerHolder(em));
		delegate.flush();
		replay(delegate);

		writer.flush();

		verify(delegate);
		verify(em);
		TransactionSynchronizationManager.unbindResource(emf);
	}

	@Test
	public void testClear() throws Exception{
		EntityManager em = createMock("em", EntityManager.class);
		em.clear();
		replay(em);
		replay(emf);
		TransactionSynchronizationManager.bindResource(emf, new EntityManagerHolder(em));
		delegate.clear();
		replay(delegate);

		writer.clear();

		verify(delegate);
		verify(em);
		TransactionSynchronizationManager.unbindResource(emf);
	}
}
