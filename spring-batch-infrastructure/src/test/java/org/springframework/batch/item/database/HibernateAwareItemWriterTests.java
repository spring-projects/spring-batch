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
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.item.support.AbstractItemWriter;
import org.springframework.batch.repeat.support.RepeatSynchronizationManager;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateTemplate;

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

	private class StubItemWriter extends AbstractItemWriter<Object> {
		public void write(List<? extends Object> items) {
			list.addAll(items);
		}
	}

	HibernateAwareItemWriter<Object> writer = new HibernateAwareItemWriter<Object>();

	final List<Object> list = new ArrayList<Object>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		writer.setDelegate(new StubItemWriter());
		writer.setHibernateTemplate(new HibernateTemplateWrapper());
		list.clear();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		RepeatSynchronizationManager.clear();
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.HibernateAwareItemWriter#afterPropertiesSet()}
	 * .
	 * 
	 * @throws Exception
	 */
	public void testAfterPropertiesSet() throws Exception {
		writer = new HibernateAwareItemWriter<Object>();
		try {
			writer.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
			assertTrue("Wrong message for exception: " + e.getMessage(), e.getMessage().indexOf("delegate") >= 0);
		}
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.HibernateAwareItemWriter#afterPropertiesSet()}
	 * .
	 * 
	 * @throws Exception
	 */
	public void testAfterPropertiesSetWithDelegate() throws Exception {
		writer.afterPropertiesSet();
	}

	public void testWriteAndFlushSunnyDay() throws Exception {
		writer.write(Collections.singletonList("foo"));
		assertEquals(3, list.size());
		assertTrue(list.contains("foo"));
		assertTrue(list.contains("flush"));
		assertTrue(list.contains("clear"));
	}

	public void testWriteAndFlushWithFailure() throws Exception {
		final RuntimeException ex = new RuntimeException("bar");
		writer.setHibernateTemplate(new HibernateTemplateWrapper() {
			public void flush() throws DataAccessException {
				throw ex;
			}
		});
		try {
			writer.write(Collections.singletonList("foo"));
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("bar", e.getMessage());
		}
		assertEquals(2, list.size());
		assertTrue(list.contains("foo"));
		writer.setHibernateTemplate(new HibernateTemplateWrapper() {
			public void flush() throws DataAccessException {
				list.add("flush");
			}
		});
		writer.write(Collections.singletonList("foo"));
		System.err.println(list);
		assertTrue(list.contains("flush"));
		assertTrue(list.contains("clear"));
	}

}
