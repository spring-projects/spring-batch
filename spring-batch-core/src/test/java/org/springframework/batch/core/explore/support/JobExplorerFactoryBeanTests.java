/*
 * Copyright 2006-2023 the original author or authors.
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

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.batch.core.job.DefaultJobKeyGenerator;
import org.springframework.batch.core.job.JobKeyGenerator;
import org.springframework.batch.core.repository.explore.JobExplorer;
import org.springframework.batch.core.repository.explore.support.JobExplorerFactoryBean;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Dave Syer
 * @author Will Schipp
 * @author Mahmoud Ben Hassine
 *
 */
@SuppressWarnings("removal")
class JobExplorerFactoryBeanTests {

	private JobExplorerFactoryBean factory;

	private final String tablePrefix = "TEST_BATCH_PREFIX_";

	@BeforeEach
	void setUp() {

		factory = new JobExplorerFactoryBean();
		DataSource dataSource = mock();
		PlatformTransactionManager transactionManager = mock();
		factory.setDataSource(dataSource);
		factory.setTransactionManager(transactionManager);
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

		JdbcOperations customJdbcOperations = mock();
		factory.setJdbcOperations(customJdbcOperations);
		factory.afterPropertiesSet();
		assertEquals(customJdbcOperations, ReflectionTestUtils.getField(factory, "jdbcOperations"));
	}

	@Test
	void testMissingDataSource() {

		factory.setDataSource(null);
		Exception exception = assertThrows(IllegalStateException.class, factory::afterPropertiesSet);
		String message = exception.getMessage();
		assertTrue(message.contains("DataSource"), "Wrong message: " + message);

	}

	@Test
	void testMissingTransactionManager() {

		factory.setTransactionManager(null);
		Exception exception = assertThrows(IllegalArgumentException.class, factory::afterPropertiesSet);
		String message = exception.getMessage();
		assertTrue(message.contains("TransactionManager"), "Wrong message: " + message);

	}

	@Test
	void testCreateExplorer() throws Exception {

		factory.afterPropertiesSet();
		JobExplorer explorer = factory.getObject();
		assertNotNull(explorer);

	}

	@Test
	public void testCustomTransactionAttributesSource() throws Exception {
		// given
		TransactionAttributeSource transactionAttributeSource = Mockito.mock();
		this.factory.setTransactionAttributeSource(transactionAttributeSource);
		this.factory.afterPropertiesSet();

		// when
		JobExplorer explorer = this.factory.getObject();

		// then
		Advised target = (Advised) explorer;
		Advisor[] advisors = target.getAdvisors();
		for (Advisor advisor : advisors) {
			if (advisor.getAdvice() instanceof TransactionInterceptor transactionInterceptor) {
				Assertions.assertEquals(transactionAttributeSource,
						transactionInterceptor.getTransactionAttributeSource());
			}
		}
	}

	@Test
	public void testDefaultJobKeyGenerator() throws Exception {
		this.factory.afterPropertiesSet();
		JobKeyGenerator jobKeyGenerator = (JobKeyGenerator) ReflectionTestUtils.getField(factory, "jobKeyGenerator");
		Assertions.assertEquals(DefaultJobKeyGenerator.class, jobKeyGenerator.getClass());
	}

	@Test
	public void testCustomJobKeyGenerator() throws Exception {
		factory.setJobKeyGenerator(new CustomJobKeyGenerator());
		this.factory.afterPropertiesSet();
		JobKeyGenerator jobKeyGenerator = (JobKeyGenerator) ReflectionTestUtils.getField(factory, "jobKeyGenerator");
		Assertions.assertEquals(CustomJobKeyGenerator.class, jobKeyGenerator.getClass());
	}

	static class CustomJobKeyGenerator implements JobKeyGenerator<String> {

		@Override
		public String generateKey(String source) {
			return "1";
		}

	}

}
