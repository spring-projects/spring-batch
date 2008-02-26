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
package org.springframework.batch.io.support;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.exception.ClearFailedException;
import org.springframework.batch.item.exception.FlushFailedException;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatListener;
import org.springframework.batch.repeat.context.RepeatContextSupport;
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

	private class StubItemWriter implements ItemWriter, RepeatListener {
		public void write(Object item) {
			list.add(item);
		}

		public void after(RepeatContext context, ExitStatus result) {
			list.add(result);
		}

		public void before(RepeatContext context) {
			list.add(context);
		}

		public void close(RepeatContext context) {
			list.add(context);
		}

		public void onError(RepeatContext context, Throwable e) {
			list.add(e);
		}

		public void open(RepeatContext context) {
			list.add(context);
		}

		public void close() throws Exception {
		}

		public void clear() throws ClearFailedException {
			list.add("clear");
		}

		public void flush() throws FlushFailedException {
			list.add("flush");
		}
	}

	HibernateAwareItemWriter writer = new HibernateAwareItemWriter();
	
	final List list = new ArrayList();

	private RepeatContextSupport context;

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		writer.setDelegate(new StubItemWriter());
		context = new RepeatContextSupport(null);
		writer.open(context);
		writer.setHibernateTemplate(new HibernateTemplateWrapper());
		list.clear();
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		Map map = TransactionSynchronizationManager.getResourceMap();
		for (Iterator iterator = map.keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			TransactionSynchronizationManager.unbindResource(key);			
		}
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.io.support.HibernateAwareItemWriter#initDao()}.
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
	 * {@link org.springframework.batch.io.support.HibernateAwareItemWriter#initDao()}.
	 * 
	 * @throws Exception
	 */
	public void testAfterPropertiesSetWithDelegate() throws Exception {
		writer.afterPropertiesSet();
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.io.support.HibernateAwareItemWriter#write(java.lang.Object)}.
	 * @throws Exception 
	 */
	public void testWrite() throws Exception {
		writer.write("foo");
		assertEquals(1, list.size());
		assertTrue(list.contains("foo"));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.io.support.HibernateAwareItemWriter#write(java.lang.Object)}.
	 */
	public void testCloseWithFailure() throws Exception{
		final RuntimeException ex = new RuntimeException("bar");
		writer.setHibernateTemplate(new HibernateTemplate() {
			public void flush() throws DataAccessException {
				throw ex;
			}
		});
		try {
			writer.close(context);
			fail("Expected RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("bar", e.getMessage());
		}
		assertEquals(1, list.size());
		System.err.println(list);
		assertTrue(list.contains(context));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.io.support.HibernateAwareItemWriter#write(java.lang.Object)}.
	 * @throws Exception 
	 */
	public void testWriteAndCloseWithFailure() throws Exception {
		final RuntimeException ex = new RuntimeException("bar");
		writer.setHibernateTemplate(new HibernateTemplateWrapper() {
			public void flush() throws DataAccessException {
				throw ex;
			}
		});
		writer.write("foo");
		try {
			writer.close(context);
			fail("Expected RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("bar", e.getMessage());
		}
		assertEquals(2, list.size());
		assertTrue(list.contains(context));
		writer.setHibernateTemplate(new HibernateTemplateWrapper() {
			public void flush() throws DataAccessException {
				list.add("flush");
			}
		});
		writer.open(context);
		writer.write("foo");
		assertEquals(6, list.size());
		assertTrue(list.contains("flush"));
		assertTrue(list.contains("clear"));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.io.support.HibernateAwareItemWriter#before(org.springframework.batch.repeat.RepeatContext)}.
	 */
	public void testBefore() {
		writer.before(context);
		assertEquals(1, list.size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.io.support.HibernateAwareItemWriter#after(org.springframework.batch.repeat.RepeatContext, org.springframework.batch.repeat.ExitStatus)}.
	 */
	public void testAfter() {
		writer.after(context, ExitStatus.FINISHED);
		assertEquals(1, list.size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.io.support.HibernateAwareItemWriter#close(org.springframework.batch.repeat.RepeatContext)}.
	 */
	public void testFlush() throws Exception{
		writer.flush();
		assertEquals(1, list.size());
		assertTrue(list.contains("flush"));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.io.support.HibernateAwareItemWriter#close(org.springframework.batch.repeat.RepeatContext)}.
	 */
	public void testCloseAfterClear() throws Exception{
		Map map = TransactionSynchronizationManager.getResourceMap();
		String key = (String) map.keySet().iterator().next();
		TransactionSynchronizationManager.unbindResource(key);
		writer.close(context);
		assertEquals(3, list.size());
		assertTrue(list.contains("flush"));
		assertTrue(list.contains("clear"));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.io.support.HibernateAwareItemWriter#onError(org.springframework.batch.repeat.RepeatContext, java.lang.Throwable)}.
	 */
	public void testOnError() {
		writer.onError(context, new Exception());
		assertEquals(1, list.size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.io.support.HibernateAwareItemWriter#open(org.springframework.batch.repeat.RepeatContext)}.
	 */
	public void testOpen() {
		writer.open(context);
		assertEquals(1, list.size());
	}

}
