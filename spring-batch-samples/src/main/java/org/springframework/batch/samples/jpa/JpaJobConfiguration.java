/*
 * Copyright 2023 the original author or authors.
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

import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.samples.common.DataSourceConfiguration;
import org.springframework.batch.samples.domain.trade.CustomerCredit;
import org.springframework.batch.samples.domain.trade.internal.CustomerCreditIncreaseProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.Isolation;

/**
 * Hibernate JPA dialect does not support custom tx isolation levels => overwrite with
 * ISOLATION_DEFAULT.
 *
 * @author Mahmoud Ben Hassine
 */
@Configuration
@Import(DataSourceConfiguration.class)
@EnableBatchProcessing
@EnableJdbcJobRepository(isolationLevelForCreate = Isolation.DEFAULT, transactionManagerRef = "jpaTransactionManager")
public class JpaJobConfiguration {

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

	@Bean
	public Job job(JobRepository jobRepository, JpaTransactionManager jpaTransactionManager,
			JpaPagingItemReader<CustomerCredit> itemReader, JpaItemWriter<CustomerCredit> itemWriter) {
		return new JobBuilder("ioSampleJob", jobRepository)
			.start(new StepBuilder("step1", jobRepository)
				.<CustomerCredit, CustomerCredit>chunk(2, jpaTransactionManager)
				.reader(itemReader)
				.processor(new CustomerCreditIncreaseProcessor())
				.writer(itemWriter)
				.build())
			.build();
	}

	// Infrastructure beans

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
