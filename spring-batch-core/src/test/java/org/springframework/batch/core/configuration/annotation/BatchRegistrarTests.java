/*
 * Copyright 2022 the original author or authors.
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
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;

import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.JdbcJobInstanceDao;
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
	@DisplayName("When no datasource is provided, then an IllegalStateException should be thrown")
	void testMissingDataSource() {
		Assertions.assertThrows(IllegalStateException.class, new Executable() {
			@Override
			public void execute() throws Throwable {
				new AnnotationConfigApplicationContext(JobConfigurationWithoutDataSource.class);
			}
		});
	}

	@Test
	@DisplayName("When no transaction manager is provided, then an IllegalStateException should be thrown")
	void testMissingTransactionManager() {
		Assertions.assertThrows(IllegalStateException.class, new Executable() {
			@Override
			public void execute() throws Throwable {
				new AnnotationConfigApplicationContext(JobConfigurationWithoutTransactionManager.class);
			}
		});
	}

	@Test
	@DisplayName("When cusotm beans are provided, then no new ones should be created")
	void testConfigurationWithUserDefinedBeans() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				JobConfigurationWithUserDefinedInfrastrucutreBeans.class);

		Assertions.assertEquals(JobConfigurationWithUserDefinedInfrastrucutreBeans.jobRepository,
				context.getBean(JobRepository.class));
		Assertions.assertEquals(JobConfigurationWithUserDefinedInfrastrucutreBeans.jobExplorer,
				context.getBean(JobExplorer.class));
		Assertions.assertEquals(JobConfigurationWithUserDefinedInfrastrucutreBeans.jobLauncher,
				context.getBean(JobLauncher.class));
		Assertions.assertEquals(JobConfigurationWithUserDefinedInfrastrucutreBeans.jobRegistry,
				context.getBean(JobRegistry.class));
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

		// TODO assert on other DAOs

		PlatformTransactionManager transactionManager = getTransactionManagerSetOnJobRepository(jobRepository);
		Assertions.assertEquals(context.getBean(JdbcTransactionManager.class), transactionManager);
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
			return Mockito.mock(DataSource.class);
		}

	}

	@Configuration
	@EnableBatchProcessing
	public static class JobConfigurationWithUserDefinedInfrastrucutreBeans {

		public static JobRepository jobRepository = Mockito.mock(JobRepository.class);

		public static JobExplorer jobExplorer = Mockito.mock(JobExplorer.class);

		public static JobLauncher jobLauncher = Mockito.mock(JobLauncher.class);

		public static JobRegistry jobRegistry = Mockito.mock(JobRegistry.class);

		@Bean
		public JobRepository jobRepository() {
			return jobRepository;
		}

		@Bean
		public JobExplorer jobExplorer() {
			return jobExplorer;
		}

		@Bean
		public JobLauncher jobLauncher() {
			return jobLauncher;
		}

		@Bean
		public JobRegistry jobRegistry() {
			return jobRegistry;
		}

	}

	@Configuration
	@EnableBatchProcessing
	public static class JobConfiguration {

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL)
					.addScript("/org/springframework/batch/core/schema-hsqldb.sql").generateUniqueName(true).build();
		}

		@Bean
		public JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
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