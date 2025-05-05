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
package org.springframework.batch.core.configuration.support;

import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.xml.DummyJobRepository;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Mahmoud Ben Hassine
 */
class DefaultBatchConfigurationTests {

	@Test
	void testDefaultConfiguration() throws Exception {
		// given
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MyJobConfiguration.class);
		Job job = context.getBean(Job.class);
		JobLauncher jobLauncher = context.getBean(JobLauncher.class);

		// when
		JobExecution jobExecution = jobLauncher.run(job, new JobParameters());

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
	}

	@Test
	void testConfigurationWithoutDataSource() {
		Assertions.assertThrows(BeanCreationException.class,
				() -> new AnnotationConfigApplicationContext(MyJobConfigurationWithoutDataSource.class));
	}

	@Test
	void testConfigurationWithoutTransactionManager() {
		Assertions.assertThrows(BeanCreationException.class,
				() -> new AnnotationConfigApplicationContext(MyJobConfigurationWithoutTransactionManager.class));
	}

	@Test
	void testConfigurationWithCustomInfrastructureBean() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				MyJobConfigurationWithCustomInfrastructureBean.class);
		Map<String, JobRepository> jobRepositories = context.getBeansOfType(JobRepository.class);
		Assertions.assertEquals(1, jobRepositories.size());
		JobRepository jobRepository = jobRepositories.entrySet().iterator().next().getValue();
		Assertions.assertInstanceOf(DummyJobRepository.class, jobRepository);
		Map<String, JobRegistrySmartInitializingSingleton> jobRegistrySmartInitializingSingletonMap = context
			.getBeansOfType(JobRegistrySmartInitializingSingleton.class);
		Assertions.assertEquals(1, jobRegistrySmartInitializingSingletonMap.size());
	}

	@Test
	void testDefaultInfrastructureBeansRegistration() {
		// given
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MyJobConfiguration.class);

		// when
		JobLauncher jobLauncher = context.getBean(JobLauncher.class);
		JobRepository jobRepository = context.getBean(JobRepository.class);
		JobRegistry jobRegistry = context.getBean(JobRegistry.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		JobRegistrySmartInitializingSingleton jobRegistrySmartInitializingSingleton = context
			.getBean(JobRegistrySmartInitializingSingleton.class);

		// then
		Assertions.assertNotNull(jobLauncher);
		Assertions.assertNotNull(jobRepository);
		Assertions.assertNotNull(jobRegistry);
		Assertions.assertNotNull(jobOperator);
		Assertions.assertNotNull(jobRegistrySmartInitializingSingleton);
	}

	@Configuration
	static class MyJobConfigurationWithoutDataSource extends DefaultBatchConfiguration {

	}

	@Configuration
	static class MyJobConfigurationWithoutTransactionManager extends DefaultBatchConfiguration {

	}

	@Configuration
	static class MyJobConfiguration extends DefaultBatchConfiguration {

		@Bean
		public Step myStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
			Tasklet myTasklet = (contribution, chunkContext) -> RepeatStatus.FINISHED;
			return new StepBuilder("myStep", jobRepository).tasklet(myTasklet, transactionManager).build();
		}

		@Bean
		public Job job(JobRepository jobRepository, Step myStep) {
			return new JobBuilder("job", jobRepository).start(myStep).build();
		}

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL)
				.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
				.generateUniqueName(true)
				.build();
		}

		@Bean
		public PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

	}

	@Configuration
	static class MyJobConfigurationWithCustomInfrastructureBean extends MyJobConfiguration {

		@Bean
		@Override
		public JobRepository jobRepository() {
			return new DummyJobRepository();
		}

		@Bean
		public JobRegistrySmartInitializingSingleton jobRegistrySmartInitializingSingleton(JobRegistry jobRegistry) {
			JobRegistrySmartInitializingSingleton smartInitializingSingleton = new JobRegistrySmartInitializingSingleton();
			smartInitializingSingleton.setJobRegistry(jobRegistry);
			return smartInitializingSingleton;
		}

	}

}