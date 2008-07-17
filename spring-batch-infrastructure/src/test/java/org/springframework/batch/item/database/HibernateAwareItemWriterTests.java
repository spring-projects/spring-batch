/*
 * Copyright 2006-2007 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.item.ClearFailedException;
import org.springframework.batch.item.FlushFailedException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.context.RepeatContextSupport;
import org.springframework.batch.repeat.support.RepeatSynchronizationManager;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Dave Syer
 * 
 */
public class HibernateAwareItemWriterTests extends TestCase {

	private class HibernateTemplateWrapper extends HibernateTemplate {
		public void flush() throws DataAccessException {
			list.add("flush");
		}
		public void clear() {
			list.add("clear");				
		};
	}

	private class StubItemWriter implements ItemWriter {
		public void write(Object item) {
			list.add(item);
		}

		public void clear() throws ClearFailedException {
			list.add("delegateClear");
		}

		public void flush() throws FlushFailedException {
			list.add("delegateFlush");
		}
	}

	HibernateAwareItemWriter writer = new HibernateAwareItemWriter();
	
	final List<Object> list = new ArrayList<Object>();

	private RepeatContextSupport context;

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		writer.setDelegate(new StubItemWriter());
		context = new RepeatContextSupport(null);
		RepeatSynchronizationManager.register(context);
		writer.setHibernateTemplate(new HibernateTemplateWrapper());
		list.clear();
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		String key = writer.getResourceKey();
		if (TransactionSynchronizationManager.hasResource(key)) {
			TransactionSynchronizationManager.unbindResource(key);	
		}
		RepeatSynchronizationManager.clear();
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.HibernateAwareItemWriter#afterPropertiesSet()}.
	 * 
	 * @throws Exception
	 */
	public void testAfterPropertiesSet() throws Exception {
		writer = new HibernateAwareItemWriter();
		try {
			writer.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// expected
			assertTrue("Wrong message for exception: " + e.getMessage(), e
					.getMessage().indexOf("delegate") >= 0);
		}
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.HibernateAwareItemWriter#afterPropertiesSet()}.
	 * 
	 * @throws Exception
	 */
	public void testAfterPropertiesSetWithDelegate() throws Exception {
		writer.afterPropertiesSet();
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.HibernateAwareItemWriter#write(java.lang.Object)}.
	 * @throws Exception 
	 */
	public void testWrite() throws Exception {
		writer.write("foo");
		assertEquals(1, list.size());
		assertTrue(list.contains("foo"));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.HibernateAwareItemWriter#write(java.lang.Object)}.
	 */
	public void testFlushWithFailure() throws Exception{
		final RuntimeException ex = new RuntimeException("bar");
		writer.setHibernateTemplate(new HibernateTemplate() {
			public void flush() throws DataAccessException {
				throw ex;
			}
		});
		try {
			writer.flush();
			fail("Expected RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("bar", e.getMessage());
		}
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.HibernateAwareItemWriter#write(java.lang.Object)}.
	 * @throws Exception 
	 */
	public void testWriteAndFlushWithFailure() throws Exception {
		final RuntimeException ex = new RuntimeException("bar");
		writer.setHibernateTemplate(new HibernateTemplateWrapper() {
			public void flush() throws DataAccessException {
				throw ex;
			}
		});
		writer.write("foo");
		try {
			writer.flush();
			fail("Expected RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("bar", e.getMessage());
		}
		assertEquals(2, list.size());
		assertTrue(list.contains("foo"));
		assertTrue(list.contains("delegateFlush"));
		writer.setHibernateTemplate(new HibernateTemplateWrapper() {
			public void flush() throws DataAccessException {
				list.add("flush");
			}
		});
		writer.write("foo");
		assertEquals(6, list.size());
		assertTrue(list.contains("flush"));
		assertTrue(list.contains("clear"));
		assertTrue(list.contains("delegateFlush"));
		assertTrue(context.isCompleteOnly());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.HibernateAwareItemWriter#flush()}.
	 */
	public void testFlush() throws Exception{
		writer.flush();
		assertEquals(3, list.size());
		assertTrue(list.contains("flush"));
		assertTrue(list.contains("clear"));
		assertTrue(list.contains("delegateFlush"));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.HibernateAwareItemWriter#clear()}.
	 */
	public void testClear() throws Exception{
		writer.clear();
		assertEquals(2, list.size());
		assertTrue(list.contains("clear"));
		assertTrue(list.contains("delegateClear"));
	}
}
