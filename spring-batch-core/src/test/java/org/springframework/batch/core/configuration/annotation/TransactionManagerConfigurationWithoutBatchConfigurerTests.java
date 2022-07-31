/*
 * Copyright 2018-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.core.configuration.annotation;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Mahmoud Ben Hassine
 */
class TransactionManagerConfigurationWithoutBatchConfigurerTests extends TransactionManagerConfigurationTests {

	@Test
	void testConfigurationWithNoDataSourceAndNoTransactionManager() {
		assertThrows(BeanCreationException.class, () -> new AnnotationConfigApplicationContext(
				BatchConfigurationWithNoDataSourceAndNoTransactionManager.class));
	}

	@Test
	void testConfigurationWithNoDataSourceAndTransactionManager() {
		assertThrows(BeanCreationException.class, () -> new AnnotationConfigApplicationContext(
				BatchConfigurationWithNoDataSourceAndTransactionManager.class));
	}

	@Test
	void testConfigurationWithDataSourceAndNoTransactionManager() throws Exception {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				BatchConfigurationWithDataSourceAndNoTransactionManager.class);
		PlatformTransactionManager platformTransactionManager = getTransactionManagerSetOnJobRepository(
				applicationContext.getBean(JobRepository.class));
		assertTrue(platformTransactionManager instanceof JdbcTransactionManager);
		JdbcTransactionManager JdbcTransactionManager = (JdbcTransactionManager) platformTransactionManager;
		assertEquals(applicationContext.getBean(DataSource.class), JdbcTransactionManager.getDataSource());
	}

	@Test
	void testConfigurationWithDataSourceAndOneTransactionManager() throws Exception {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				BatchConfigurationWithDataSourceAndOneTransactionManager.class);
		PlatformTransactionManager platformTransactionManager = applicationContext
				.getBean(PlatformTransactionManager.class);
		assertSame(transactionManager, platformTransactionManager);
		// In this case, the supplied transaction manager won't be used by batch and a
		// JdbcTransactionManager will be used instead.
		// The user has to provide a custom BatchConfigurer.
		assertTrue(getTransactionManagerSetOnJobRepository(
				applicationContext.getBean(JobRepository.class)) instanceof JdbcTransactionManager);
	}

	@Test
	void testConfigurationWithDataSourceAndMultipleTransactionManagers() throws Exception {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				BatchConfigurationWithDataSourceAndMultipleTransactionManagers.class);
		PlatformTransactionManager platformTransactionManager = applicationContext
				.getBean(PlatformTransactionManager.class);
		assertSame(transactionManager2, platformTransactionManager);
		// In this case, the supplied primary transaction manager won't be used by batch
		// and a JdbcTransactionManager will be used instead.
		// The user has to provide a custom BatchConfigurer.
		assertTrue(getTransactionManagerSetOnJobRepository(
				applicationContext.getBean(JobRepository.class)) instanceof JdbcTransactionManager);
	}

	@Configuration
	@EnableBatchProcessing
	public static class BatchConfigurationWithNoDataSourceAndNoTransactionManager {

	}

	@Configuration
	@EnableBatchProcessing
	public static class BatchConfigurationWithNoDataSourceAndTransactionManager {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return transactionManager;
		}

	}

	@Configuration
	@EnableBatchProcessing
	public static class BatchConfigurationWithDataSourceAndNoTransactionManager {

		@Bean
		public DataSource dataSource() {
			return createDataSource();
		}

	}

	@Configuration
	@EnableBatchProcessing
	public static class BatchConfigurationWithDataSourceAndOneTransactionManager {

		@Bean
		public DataSource dataSource() {
			return createDataSource();
		}

		@Bean
		public PlatformTransactionManager transactionManager() {
			return transactionManager;
		}

	}

	@Configuration
	@EnableBatchProcessing
	public static class BatchConfigurationWithDataSourceAndMultipleTransactionManagers {

		@Bean
		public DataSource dataSource() {
			return createDataSource();
		}

		@Bean
		public PlatformTransactionManager transactionManager() {
			return transactionManager;
		}

		@Primary
		@Bean
		public PlatformTransactionManager transactionManager2() {
			return transactionManager2;
		}

	}

}
