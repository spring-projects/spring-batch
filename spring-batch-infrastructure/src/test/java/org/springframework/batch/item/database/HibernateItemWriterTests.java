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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateOperations;

/**
 * @author Dave Syer
 * @author Thomas Risberg
 */
public class HibernateItemWriterTests {

	HibernateOperations ht;

	HibernateItemWriter<Object> writer;

	@Before
	public void setUp() throws Exception {
		writer = new HibernateItemWriter<Object>();
		ht = createMock("ht", HibernateOperations.class);
		writer.setHibernateTemplate(ht);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.HibernateItemWriter#afterPropertiesSet()}
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAfterPropertiesSet() throws Exception {
		writer = new HibernateItemWriter<Object>();
		try {
			writer.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
			assertTrue("Wrong message for exception: " + e.getMessage(), e.getMessage().indexOf("HibernateOperations") >= 0);
		}
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.database.HibernateItemWriter#afterPropertiesSet()}
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAfterPropertiesSetWithDelegate() throws Exception {
		writer.afterPropertiesSet();
	}

	@Test
	public void testWriteAndFlushSunnyDay() throws Exception {
		ht.contains("foo");
		expectLastCall().andReturn(true);
		ht.contains("bar");
		expectLastCall().andReturn(false);
		ht.saveOrUpdate("bar");
		ht.flush();
		ht.clear();
		replay(ht);
		
		List<String> items = Arrays.asList(new String[] { "foo", "bar" });
		writer.write(items);
		
		verify(ht);
	}

	@Test
	public void testWriteAndFlushWithFailure() throws Exception {
		final RuntimeException ex = new RuntimeException("ERROR");
		ht.contains("foo");
		expectLastCall().andThrow(ex);
		replay(ht);
		
		try {
			writer.write(Collections.singletonList("foo"));
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("ERROR", e.getMessage());
		}

		verify(ht);
	}

}
