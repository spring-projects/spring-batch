/*
 * Copyright 2018 the original author or authors.
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

import org.junit.Assert;
import org.junit.Test;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Mahmoud Ben Hassine
 */
public class TransactionManagerConfigurationWithoutBatchConfigurerTests extends TransactionManagerConfigurationTests {

	@Test
	public void testConfigurationWithNoDataSourceAndNoTransactionManager() throws Exception {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(BatchConfigurationWithNoDataSourceAndNoTransactionManager.class);
		Assert.assertTrue(applicationContext.containsBean("transactionManager"));
		PlatformTransactionManager platformTransactionManager = applicationContext.getBean(PlatformTransactionManager.class);
		Object targetObject = AopTestUtils.getTargetObject(platformTransactionManager);
		Assert.assertTrue(targetObject instanceof ResourcelessTransactionManager);
		Assert.assertSame(getTransactionManagerSetOnJobRepository(applicationContext.getBean(JobRepository.class)), targetObject);
	}

	@Test
	public void testConfigurationWithNoDataSourceAndTransactionManager() throws Exception {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(BatchConfigurationWithNoDataSourceAndTransactionManager.class);
		PlatformTransactionManager platformTransactionManager = applicationContext.getBean(PlatformTransactionManager.class);
		Assert.assertSame(transactionManager, platformTransactionManager);
		// In this case, the supplied transaction manager won't be used by batch and a ResourcelessTransactionManager will be used instead.
		// The user has to provide a custom BatchConfigurer.
		Assert.assertTrue(getTransactionManagerSetOnJobRepository(applicationContext.getBean(JobRepository.class)) instanceof ResourcelessTransactionManager);
	}

	@Test
	public void testConfigurationWithDataSourceAndNoTransactionManager() throws Exception {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(BatchConfigurationWithDataSourceAndNoTransactionManager.class);
		Assert.assertTrue(applicationContext.containsBean("transactionManager"));
		PlatformTransactionManager platformTransactionManager = applicationContext.getBean(PlatformTransactionManager.class);
		Object targetObject = AopTestUtils.getTargetObject(platformTransactionManager);
		Assert.assertTrue(targetObject instanceof DataSourceTransactionManager);
		DataSourceTransactionManager dataSourceTransactionManager = (DataSourceTransactionManager) targetObject;
		Assert.assertEquals(applicationContext.getBean(DataSource.class), dataSourceTransactionManager.getDataSource());
		Assert.assertSame(getTransactionManagerSetOnJobRepository(applicationContext.getBean(JobRepository.class)), dataSourceTransactionManager);
	}

	@Test
	public void testConfigurationWithDataSourceAndOneTransactionManager() throws Exception {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(BatchConfigurationWithDataSourceAndOneTransactionManager.class);
		PlatformTransactionManager platformTransactionManager = applicationContext.getBean(PlatformTransactionManager.class);
		Assert.assertSame(transactionManager, platformTransactionManager);
		// In this case, the supplied transaction manager won't be used by batch and a DataSourceTransactionManager will be used instead.
		// The user has to provide a custom BatchConfigurer.
		Assert.assertTrue(getTransactionManagerSetOnJobRepository(applicationContext.getBean(JobRepository.class)) instanceof DataSourceTransactionManager);
	}

	@Test
	public void testConfigurationWithDataSourceAndMultipleTransactionManagers() throws Exception {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(BatchConfigurationWithDataSourceAndMultipleTransactionManagers.class);
		PlatformTransactionManager platformTransactionManager = applicationContext.getBean(PlatformTransactionManager.class);
		Assert.assertSame(transactionManager2, platformTransactionManager);
		// In this case, the supplied primary transaction manager won't be used by batch and a DataSourceTransactionManager will be used instead.
		// The user has to provide a custom BatchConfigurer.
		Assert.assertTrue(getTransactionManagerSetOnJobRepository(applicationContext.getBean(JobRepository.class)) instanceof DataSourceTransactionManager);

	}

	@EnableBatchProcessing
	public static class BatchConfigurationWithNoDataSourceAndNoTransactionManager {

	}

	@EnableBatchProcessing
	public static class BatchConfigurationWithNoDataSourceAndTransactionManager {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return transactionManager;
		}
	}

	@EnableBatchProcessing
	public static class BatchConfigurationWithDataSourceAndNoTransactionManager {

		@Bean
		public DataSource dataSource() {
			return createDataSource();
		}
	}

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
