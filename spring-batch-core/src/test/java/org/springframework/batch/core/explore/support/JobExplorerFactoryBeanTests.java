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
package org.springframework.batch.core.explore.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Dave Syer
 * @author Will Schipp
 *
 */
class JobExplorerFactoryBeanTests {

	private JobExplorerFactoryBean factory;

	private final String tablePrefix = "TEST_BATCH_PREFIX_";

	@BeforeEach
	void setUp() {

		factory = new JobExplorerFactoryBean();
		DataSource dataSource = mock(DataSource.class);
		factory.setDataSource(dataSource);
		factory.setTablePrefix(tablePrefix);

	}

	@Test
	void testDefaultJdbcOperations() throws Exception {

		factory.afterPropertiesSet();
		JdbcOperations jdbcOperations = (JdbcOperations) ReflectionTestUtils.getField(factory, "jdbcOperations");
		assertTrue(jdbcOperations instanceof JdbcTemplate);
	}

	@Test
	void testCustomJdbcOperations() throws Exception {

		JdbcOperations customJdbcOperations = mock(JdbcOperations.class);
		factory.setJdbcOperations(customJdbcOperations);
		factory.afterPropertiesSet();
		assertEquals(customJdbcOperations, ReflectionTestUtils.getField(factory, "jdbcOperations"));
	}

	@Test
	void testMissingDataSource() {

		factory.setDataSource(null);
		Exception exception = assertThrows(IllegalArgumentException.class, factory::afterPropertiesSet);
		String message = exception.getMessage();
		assertTrue(message.contains("DataSource"), "Wrong message: " + message);

	}

	@Test
	void testCreateExplorer() throws Exception {

		factory.afterPropertiesSet();
		JobExplorer explorer = factory.getObject();
		assertNotNull(explorer);

	}

}
