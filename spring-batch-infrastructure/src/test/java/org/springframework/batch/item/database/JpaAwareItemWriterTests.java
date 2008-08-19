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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ItemWriter;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
	@SuppressWarnings( { "unchecked" })
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
		}
		catch (IllegalArgumentException e) {
			// expected
			assertTrue("Wrong message for exception: " + e.getMessage(), e.getMessage().indexOf("delegate") >= 0);
		}
		writer.setDelegate(delegate);
		try {
			writer.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
			assertTrue("Wrong message for exception: " + e.getMessage(),
					e.getMessage().indexOf("EntityManagerFactory") >= 0);
		}
	}

	@Test
	public void testWriteAndFlushSunnyDay() throws Exception {
		EntityManager em = createMock("em", EntityManager.class);
		em.flush();
		em.clear();
		replay(em);
		replay(emf);
		TransactionSynchronizationManager.bindResource(emf, new EntityManagerHolder(em));
		List<String> items = Arrays.asList(new String[] { "foo", "spam" });
		delegate.write(items);
		replay(delegate);

		writer.write(items);

		verify(delegate);
		verify(em);
		TransactionSynchronizationManager.unbindResource(emf);
	}

	@Test
	public void testWriteAndFlushWithFailure() throws Exception {
		final RuntimeException ex = new RuntimeException("bar");
		EntityManager em = createMock("em", EntityManager.class);
		em.flush();
		expectLastCall().andThrow(ex);
		em.clear();
		replay(em);
		replay(emf);
		TransactionSynchronizationManager.bindResource(emf, new EntityManagerHolder(em));
		List<String> items = Arrays.asList(new String[] { "foo", "spam" });
		delegate.write(items);
		replay(delegate);

		try {
			writer.write(items);
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("bar", e.getMessage());
		}

		verify(delegate);
		verify(em);
		TransactionSynchronizationManager.unbindResource(emf);
	}

}
