/*
 * Copyright 2022-2024 the original author or authors.
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
package org.springframework.batch.core.configuration.annotation;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.batch.core.DefaultJobKeyGenerator;
import org.springframework.batch.core.JobKeyGenerator;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.JobRegistrySmartInitializingSingleton;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.JdbcExecutionContextDao;
import org.springframework.batch.core.repository.dao.JdbcJobExecutionDao;
import org.springframework.batch.core.repository.dao.JdbcJobInstanceDao;
import org.springframework.batch.core.repository.dao.JdbcStepExecutionDao;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * Test class for {@link BatchRegistrar}.
 *
 * @author Mahmoud Ben Hassine
 */
class BatchRegistrarTests {

	@Test
	@DisplayName("When no datasource is provided, then an BeanCreationException should be thrown")
	void testMissingDataSource() {
		Assertions.assertThrows(BeanCreationException.class,
				() -> new AnnotationConfigApplicationContext(JobConfigurationWithoutDataSource.class));
	}

	@Test
	@DisplayName("When no transaction manager is provided, then an BeanCreationException should be thrown")
	void testMissingTransactionManager() {
		Assertions.assertThrows(BeanCreationException.class,
				() -> new AnnotationConfigApplicationContext(JobConfigurationWithoutTransactionManager.class));
	}

	@Test
	@DisplayName("When custom beans are provided, then default ones should not be used")
	void testConfigurationWithUserDefinedBeans() {
		var context = new AnnotationConfigApplicationContext(JobConfigurationWithUserDefinedInfrastructureBeans.class);

		Assertions.assertTrue(Mockito.mockingDetails(context.getBean(JobRepository.class)).isMock());
		Assertions.assertTrue(Mockito.mockingDetails(context.getBean(JobExplorer.class)).isMock());
		Assertions.assertTrue(Mockito.mockingDetails(context.getBean(JobLauncher.class)).isMock());
		Assertions.assertTrue(Mockito.mockingDetails(context.getBean(JobRegistry.class)).isMock());
		Assertions.assertTrue(Mockito.mockingDetails(context.getBean(JobOperator.class)).isMock());
		Assertions
			.assertTrue(Mockito.mockingDetails(context.getBean(JobRegistrySmartInitializingSingleton.class)).isMock());
	}

	@Test
	@DisplayName("When a datasource and a transaction manager are provided, then they should be set on the job repository")
	void testDataSourceAndTransactionManagerSetup() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(JobConfiguration.class);

		JobRepository jobRepository = context.getBean(JobRepository.class);
		JdbcJobInstanceDao jobInstanceDao = (JdbcJobInstanceDao) ReflectionTestUtils.getField(jobRepository,
				"jobInstanceDao");
		JdbcTemplate jdbcTemplate = (JdbcTemplate) ReflectionTestUtils.getField(jobInstanceDao, "jdbcTemplate");
		DataSource dataSource = (DataSource) ReflectionTestUtils.getField(jdbcTemplate, "dataSource");
		Assertions.assertEquals(context.getBean(DataSource.class), dataSource);

		JdbcJobExecutionDao jobExecutionDao = (JdbcJobExecutionDao) ReflectionTestUtils.getField(jobRepository,
				"jobExecutionDao");
		jdbcTemplate = (JdbcTemplate) ReflectionTestUtils.getField(jobExecutionDao, "jdbcTemplate");
		dataSource = (DataSource) ReflectionTestUtils.getField(jdbcTemplate, "dataSource");
		Assertions.assertEquals(context.getBean(DataSource.class), dataSource);

		JdbcStepExecutionDao stepExecutionDao = (JdbcStepExecutionDao) ReflectionTestUtils.getField(jobRepository,
				"stepExecutionDao");
		jdbcTemplate = (JdbcTemplate) ReflectionTestUtils.getField(stepExecutionDao, "jdbcTemplate");
		dataSource = (DataSource) ReflectionTestUtils.getField(jdbcTemplate, "dataSource");
		Assertions.assertEquals(context.getBean(DataSource.class), dataSource);

		JdbcExecutionContextDao executionContextDao = (JdbcExecutionContextDao) ReflectionTestUtils
			.getField(jobRepository, "ecDao");
		jdbcTemplate = (JdbcTemplate) ReflectionTestUtils.getField(executionContextDao, "jdbcTemplate");
		dataSource = (DataSource) ReflectionTestUtils.getField(jdbcTemplate, "dataSource");
		Assertions.assertEquals(context.getBean(DataSource.class), dataSource);

		PlatformTransactionManager transactionManager = getTransactionManagerSetOnJobRepository(jobRepository);
		Assertions.assertEquals(context.getBean(JdbcTransactionManager.class), transactionManager);
	}

	@Test
	@DisplayName("When custom bean names are provided, then corresponding beans should be used to configure infrastructure beans")
	void testConfigurationWithCustomBeanNames() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				JobConfigurationWithCustomBeanNames.class);

		JobRepository jobRepository = context.getBean(JobRepository.class);
		JdbcJobInstanceDao jobInstanceDao = (JdbcJobInstanceDao) ReflectionTestUtils.getField(jobRepository,
				"jobInstanceDao");
		JdbcTemplate jdbcTemplate = (JdbcTemplate) ReflectionTestUtils.getField(jobInstanceDao, "jdbcTemplate");
		DataSource dataSource = (DataSource) ReflectionTestUtils.getField(jdbcTemplate, "dataSource");
		Assertions.assertEquals(context.getBean(DataSource.class), dataSource);

		JdbcJobExecutionDao jobExecutionDao = (JdbcJobExecutionDao) ReflectionTestUtils.getField(jobRepository,
				"jobExecutionDao");
		jdbcTemplate = (JdbcTemplate) ReflectionTestUtils.getField(jobExecutionDao, "jdbcTemplate");
		dataSource = (DataSource) ReflectionTestUtils.getField(jdbcTemplate, "dataSource");
		Assertions.assertEquals(context.getBean(DataSource.class), dataSource);

		JdbcStepExecutionDao stepExecutionDao = (JdbcStepExecutionDao) ReflectionTestUtils.getField(jobRepository,
				"stepExecutionDao");
		jdbcTemplate = (JdbcTemplate) ReflectionTestUtils.getField(stepExecutionDao, "jdbcTemplate");
		dataSource = (DataSource) ReflectionTestUtils.getField(jdbcTemplate, "dataSource");
		Assertions.assertEquals(context.getBean(DataSource.class), dataSource);

		JdbcExecutionContextDao executionContextDao = (JdbcExecutionContextDao) ReflectionTestUtils
			.getField(jobRepository, "ecDao");
		jdbcTemplate = (JdbcTemplate) ReflectionTestUtils.getField(executionContextDao, "jdbcTemplate");
		dataSource = (DataSource) ReflectionTestUtils.getField(jdbcTemplate, "dataSource");
		Assertions.assertEquals(context.getBean(DataSource.class), dataSource);

		PlatformTransactionManager transactionManager = getTransactionManagerSetOnJobRepository(jobRepository);
		Assertions.assertEquals(context.getBean(JdbcTransactionManager.class), transactionManager);
	}

	@Test
	void testDefaultInfrastructureBeansRegistration() {
		// given
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(JobConfiguration.class);

		// when
		JobLauncher jobLauncher = context.getBean(JobLauncher.class);
		JobRepository jobRepository = context.getBean(JobRepository.class);
		JobExplorer jobExplorer = context.getBean(JobExplorer.class);
		JobRegistry jobRegistry = context.getBean(JobRegistry.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		JobRegistrySmartInitializingSingleton jobRegistrySmartInitializingSingleton = context
			.getBean(JobRegistrySmartInitializingSingleton.class);

		// then
		Assertions.assertNotNull(jobLauncher);
		Assertions.assertNotNull(jobRepository);
		Assertions.assertNotNull(jobExplorer);
		Assertions.assertNotNull(jobRegistry);
		Assertions.assertNotNull(jobOperator);
		Assertions.assertNotNull(jobRegistrySmartInitializingSingleton);
	}

	@Test
	@DisplayName("When no JobKeyGenerator is provided the default implementation should be used")
	public void testDefaultJobKeyGeneratorConfiguration() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(JobConfiguration.class);

		JobRepository jobRepository = context.getBean(JobRepository.class);
		JdbcJobInstanceDao jobInstanceDao = (JdbcJobInstanceDao) ReflectionTestUtils.getField(jobRepository,
				"jobInstanceDao");
		JobKeyGenerator jobKeyGenerator = (JobKeyGenerator) ReflectionTestUtils.getField(jobInstanceDao,
				"jobKeyGenerator");

		Assertions.assertEquals(DefaultJobKeyGenerator.class, jobKeyGenerator.getClass());
	}

	@Test
	@DisplayName("When a custom JobKeyGenerator implementation is found that should be used")
	public void testCustomJobKeyGeneratorConfiguration() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				CustomJobKeyGeneratorConfiguration.class);

		JobRepository jobRepository = context.getBean(JobRepository.class);
		JdbcJobInstanceDao jobInstanceDao = (JdbcJobInstanceDao) ReflectionTestUtils.getField(jobRepository,
				"jobInstanceDao");
		JobKeyGenerator jobKeyGenerator = (JobKeyGenerator) ReflectionTestUtils.getField(jobInstanceDao,
				"jobKeyGenerator");
		Assertions.assertEquals(CustomJobKeyGeneratorConfiguration.TestCustomJobKeyGenerator.class,
				jobKeyGenerator.getClass());
	}

	@Configuration
	@EnableBatchProcessing
	public static class JobConfigurationWithoutDataSource {

	}

	@Configuration
	@EnableBatchProcessing
	public static class JobConfigurationWithoutTransactionManager {

		@Bean
		public DataSource dataSource() {
			return Mockito.mock();
		}

	}

	@Configuration
	@EnableBatchProcessing
	public static class JobConfigurationWithUserDefinedInfrastructureBeans {

		@Bean
		public JobRepository jobRepository() {
			return Mockito.mock();
		}

		@Bean
		public JobExplorer jobExplorer() {
			return Mockito.mock();
		}

		@Bean
		public JobLauncher jobLauncher() {
			return Mockito.mock();
		}

		@Bean
		public JobRegistry jobRegistry() {
			return Mockito.mock();
		}

		@Bean
		public JobOperator jobOperator() {
			return Mockito.mock();
		}

		@Bean
		public JobRegistrySmartInitializingSingleton jobRegistrySmartInitializingSingleton() {
			return Mockito.mock();
		}

	}

	@Configuration
	@EnableBatchProcessing
	public static class JobConfiguration {

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL)
				.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
				.generateUniqueName(true)
				.build();
		}

		@Bean
		public JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

	}

	@Configuration
	@EnableBatchProcessing(dataSourceRef = "batchDataSource", transactionManagerRef = "batchTransactionManager")
	public static class JobConfigurationWithCustomBeanNames {

		@Bean
		public DataSource batchDataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL)
				.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
				.generateUniqueName(true)
				.build();
		}

		@Bean
		public JdbcTransactionManager batchTransactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

	}

	@Configuration
	@EnableBatchProcessing
	public static class CustomJobKeyGeneratorConfiguration {

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL)
				.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
				.generateUniqueName(true)
				.build();
		}

		@Bean
		public JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

		@Bean
		public JobKeyGenerator jobKeyGenerator() {
			return new TestCustomJobKeyGenerator();
		}

		private class TestCustomJobKeyGenerator implements JobKeyGenerator {

			@Override
			public String generateKey(Object source) {
				return "1";
			}

		}

	}

	private PlatformTransactionManager getTransactionManagerSetOnJobRepository(JobRepository jobRepository) {
		Advised target = (Advised) jobRepository; // proxy created by
													// AbstractJobRepositoryFactoryBean
		Advisor[] advisors = target.getAdvisors();
		for (Advisor advisor : advisors) {
			if (advisor.getAdvice() instanceof TransactionInterceptor transactionInterceptor) {
				return (PlatformTransactionManager) transactionInterceptor.getTransactionManager();
			}
		}
		return null;
	}

}