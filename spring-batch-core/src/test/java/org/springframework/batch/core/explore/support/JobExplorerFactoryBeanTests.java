/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.core.explore.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Dave Syer
 * @author Will Schipp
 * 
 */
public class JobExplorerFactoryBeanTests {

	private JobExplorerFactoryBean factory;

	private DataSource dataSource;

	private String tablePrefix = "TEST_BATCH_PREFIX_";

	@Before
	public void setUp() throws Exception {

		factory = new JobExplorerFactoryBean();
		dataSource = mock(DataSource.class);
		factory.setDataSource(dataSource);
		factory.setTablePrefix(tablePrefix);

	}
	
	
	@Test
	public void testDefaultJdbcOperations() throws Exception {
		
		factory.afterPropertiesSet();
		JdbcOperations jdbcOperations = (JdbcOperations) ReflectionTestUtils.getField(factory, "jdbcOperations");
		assertTrue(jdbcOperations instanceof JdbcTemplate);
	}	

	@Test
	public void testCustomJdbcOperations() throws Exception {
		
		JdbcOperations customJdbcOperations = mock(JdbcOperations.class);
		factory.setJdbcOperations(customJdbcOperations);
		factory.afterPropertiesSet();
		assertEquals(customJdbcOperations, ReflectionTestUtils.getField(factory, "jdbcOperations"));
	}		

	@Test
	public void testMissingDataSource() throws Exception {

		factory.setDataSource(null);
		try {
			factory.afterPropertiesSet();
			fail();
		}
		catch (IllegalArgumentException ex) {
			// expected
			String message = ex.getMessage();
			assertTrue("Wrong message: " + message, message.indexOf("DataSource") >= 0);
		}

	}

	@Test
	public void testCreateExplorer() throws Exception {

		factory.afterPropertiesSet();
		JobExplorer explorer = factory.getObject();
		assertNotNull(explorer);

	}

}
