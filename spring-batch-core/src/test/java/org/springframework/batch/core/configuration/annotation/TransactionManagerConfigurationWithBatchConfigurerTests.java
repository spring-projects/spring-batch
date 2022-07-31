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
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Mahmoud Ben Hassine
 */
class TransactionManagerConfigurationWithBatchConfigurerTests extends TransactionManagerConfigurationTests {

	@Test
	void testConfigurationWithDataSourceAndNoTransactionManager() throws Exception {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				BatchConfigurationWithDataSourceAndNoTransactionManager.class);
		BatchConfigurer batchConfigurer = applicationContext.getBean(BatchConfigurer.class);

		PlatformTransactionManager platformTransactionManager = batchConfigurer.getTransactionManager();
		assertTrue(platformTransactionManager instanceof JdbcTransactionManager);
		JdbcTransactionManager JdbcTransactionManager = AopTestUtils.getTargetObject(platformTransactionManager);
		assertEquals(applicationContext.getBean(DataSource.class), JdbcTransactionManager.getDataSource());
		assertSame(getTransactionManagerSetOnJobRepository(applicationContext.getBean(JobRepository.class)),
				platformTransactionManager);
	}

	@Test
	void testConfigurationWithDataSourceAndTransactionManager() throws Exception {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				BatchConfigurationWithDataSourceAndTransactionManager.class);
		BatchConfigurer batchConfigurer = applicationContext.getBean(BatchConfigurer.class);

		PlatformTransactionManager platformTransactionManager = batchConfigurer.getTransactionManager();
		assertSame(transactionManager, platformTransactionManager);
		assertSame(getTransactionManagerSetOnJobRepository(applicationContext.getBean(JobRepository.class)),
				transactionManager);
	}

	@Configuration
	@EnableBatchProcessing
	public static class BatchConfigurationWithDataSourceAndNoTransactionManager {

		@Bean
		public DataSource dataSource() {
			return createDataSource();
		}

		@Bean
		public BatchConfigurer batchConfigurer(DataSource dataSource) {
			return new DefaultBatchConfigurer(dataSource);
		}

	}

	@Configuration
	@EnableBatchProcessing
	public static class BatchConfigurationWithDataSourceAndTransactionManager {

		@Bean
		public DataSource dataSource() {
			return createDataSource();
		}

		@Bean
		public PlatformTransactionManager transactionManager() {
			return transactionManager;
		}

		@Bean
		public BatchConfigurer batchConfigurer(DataSource dataSource) {
			return new DefaultBatchConfigurer(dataSource) {
				@Override
				public PlatformTransactionManager getTransactionManager() {
					return transactionManager();
				}
			};
		}

	}

}
