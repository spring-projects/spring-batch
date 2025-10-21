/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.batch.samples.jpa;

import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JpaItemWriter;
import org.springframework.batch.infrastructure.item.database.JpaPagingItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.infrastructure.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.samples.domain.trade.CustomerCredit;
import org.springframework.batch.samples.domain.trade.internal.CustomerCreditIncreaseProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Mahmoud Ben Hassine
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringJUnitConfig
class PostgreSQLJpaIntegrationTests {

	// TODO find the best way to externalize and manage image versions
	private static final DockerImageName POSTGRESQL_IMAGE = DockerImageName.parse("postgres:17.5");

	@Container
	public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRESQL_IMAGE);

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JobOperator jobOperator;

	@Autowired
	private Job job;

	@BeforeEach
	void setUp() {
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		databasePopulator.addScript(new ClassPathResource("/org/springframework/batch/core/schema-postgresql.sql"));
		databasePopulator.addScript(
				new ClassPathResource("/org/springframework/batch/samples/common/business-schema-postgresql.sql"));
		databasePopulator.execute(this.dataSource);
	}

	@Test
	void testJobExecution() throws Exception {
		// given
		JobParameters jobParameters = new JobParametersBuilder().toJobParameters();

		// when
		JobExecution jobExecution = this.jobOperator.start(this.job, jobParameters);

		// then
		assertNotNull(jobExecution);
		assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
	}

	@Configuration
	@EnableBatchProcessing
	@EnableJdbcJobRepository(transactionManagerRef = "jpaTransactionManager")
	static class TestConfiguration {

		@Bean
		public Job job(JobRepository jobRepository, JpaTransactionManager jpaTransactionManager,
				JpaPagingItemReader<CustomerCredit> itemReader, JpaItemWriter<CustomerCredit> itemWriter) {
			return new JobBuilder("ioSampleJob", jobRepository)
				.start(new StepBuilder("step1", jobRepository).<CustomerCredit, CustomerCredit>chunk(2)
					.transactionManager(jpaTransactionManager)
					.reader(itemReader)
					.processor(new CustomerCreditIncreaseProcessor())
					.writer(itemWriter)
					.build())
				.build();
		}

		@Bean
		public JpaPagingItemReader<CustomerCredit> itemReader(EntityManagerFactory entityManagerFactory) {
			return new JpaPagingItemReaderBuilder<CustomerCredit>().name("itemReader")
				.entityManagerFactory(entityManagerFactory)
				.queryString("select c from CustomerCredit c")
				.build();
		}

		@Bean
		public JpaItemWriter<CustomerCredit> itemWriter(EntityManagerFactory entityManagerFactory) {
			return new JpaItemWriterBuilder<CustomerCredit>().entityManagerFactory(entityManagerFactory).build();
		}

		// infrastructure beans

		@Bean
		public DataSource dataSource() throws Exception {
			PGSimpleDataSource datasource = new PGSimpleDataSource();
			datasource.setURL(postgres.getJdbcUrl());
			datasource.setUser(postgres.getUsername());
			datasource.setPassword(postgres.getPassword());
			return datasource;
		}

		@Bean
		public JpaTransactionManager jpaTransactionManager(EntityManagerFactory entityManagerFactory) {
			return new JpaTransactionManager(entityManagerFactory);
		}

		@Bean
		public EntityManagerFactory entityManagerFactory(PersistenceUnitManager persistenceUnitManager,
				DataSource dataSource) {
			LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
			factoryBean.setDataSource(dataSource);
			factoryBean.setPersistenceUnitManager(persistenceUnitManager);
			factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
			factoryBean.afterPropertiesSet();
			return factoryBean.getObject();
		}

		@Bean
		public PersistenceUnitManager persistenceUnitManager(DataSource dataSource) {
			DefaultPersistenceUnitManager persistenceUnitManager = new DefaultPersistenceUnitManager();
			persistenceUnitManager.setDefaultDataSource(dataSource);
			persistenceUnitManager.afterPropertiesSet();
			return persistenceUnitManager;
		}

	}

}
