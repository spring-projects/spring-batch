/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.batch.core.step.item;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.core.step.skip.AlwaysSkipItemSkipPolicy;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.JpaItemWriter;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.jdbc.JdbcTestUtils;

/**
 * Integration tests for the fault-tolerance features of {@link ChunkOrientedStep} with a
 * JPA setup.
 *
 * @author Mahmoud Ben Hassine
 */
public class ChunkOrientedStepFaultToleranceJpaIntegrationTests {

	@Test
	void testFaultTolerantChunkOrientedStep() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(JobConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);
		JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

		// when
		JobParameters jobParameters = new JobParameters();
		JobExecution jobExecution = jobOperator.start(job, jobParameters);

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		ExitStatus stepExecutionExitStatus = stepExecution.getExitStatus();
		Assertions.assertEquals(ExitStatus.COMPLETED.getExitCode(), stepExecutionExitStatus.getExitCode());
		Assertions.assertEquals(6, stepExecution.getReadCount());
		Assertions.assertEquals(4, stepExecution.getWriteCount());
		Assertions.assertEquals(0, stepExecution.getReadSkipCount());
		Assertions.assertEquals(2, stepExecution.getWriteSkipCount());
		Assertions.assertEquals(2, stepExecution.getSkipCount());
		Assertions.assertEquals(4, JdbcTestUtils.countRowsInTable(jdbcTemplate, "person_target"));
	}

	/*
	 * Regression test for https://github.com/spring-projects/spring-batch/issues/5509: a
	 * retry configured for an exception that marks the transaction rollback-only (eg an
	 * OptimisticLockException raised by a JPA flush) is retried in place, in the same
	 * transaction. If the retried write no longer throws (because the persistence context
	 * already reflects the failed attempt, so there is nothing left to flush), the chunk
	 * write must not be counted as written and committed: the transaction it ran in is
	 * still rolled back, so the step must fail instead of silently discarding the write.
	 */
	@Test
	void testChunkFailsWhenInPlaceRetrySucceedsOnAnAlreadyDoomedTransaction() throws Exception {
		// given: a row that a concurrent process will update between the writer's read
		// and its flush, so that the writer's own flush fails with an
		// OptimisticLockException and marks the transaction rollback-only
		ApplicationContext context = new AnnotationConfigApplicationContext(OptimisticLockingJobConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		Job job = context.getBean(Job.class);
		JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
		jdbcTemplate.update("insert into person_target (id, name, version) values (1, 'v1', 0)");

		// when
		JobExecution jobExecution = jobOperator.start(job, new JobParameters());

		// then: the in-place retry "succeeds" without an exception, but the transaction
		// it ran in is rolled back, so the step must fail instead of reporting COMPLETED
		// for a write that was never actually persisted
		StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
		Assertions.assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		Assertions.assertEquals(0, stepExecution.getWriteCount());
		Assertions.assertEquals(0, stepExecution.getCommitCount());
		Assertions.assertEquals(1, stepExecution.getRollbackCount());
		String persistedName = jdbcTemplate.queryForObject("select name from person_target where id = 1", String.class);
		Assertions.assertEquals("v1", persistedName);
	}

	@Configuration
	@EnableBatchProcessing
	@EnableJdbcJobRepository
	@Import(JpaInfrastructureConfiguration.class)
	static class OptimisticLockingJobConfiguration {

		// Override the bean imported from JpaInfrastructureConfiguration to match
		// the Hibernate JDBC batching setup from the issue's own reproducer
		@Bean
		public EntityManagerFactory entityManagerFactory(DataSource dataSource) {
			String packageToScan = "org.springframework.batch.core.step.item";

			DefaultPersistenceUnitManager persistenceUnitManager = new DefaultPersistenceUnitManager();
			persistenceUnitManager.setDefaultDataSource(dataSource);
			persistenceUnitManager.setPackagesToScan(packageToScan);
			persistenceUnitManager.afterPropertiesSet();

			LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
			factoryBean.setDataSource(dataSource);
			factoryBean.setPersistenceUnitManager(persistenceUnitManager);
			factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
			factoryBean.setPackagesToScan(packageToScan);
			// matches the JDBC batching setup from the issue's own reproducer
			Properties jpaProperties = new Properties();
			jpaProperties.put("hibernate.jdbc.batch_size", "30");
			factoryBean.setJpaProperties(jpaProperties);
			factoryBean.afterPropertiesSet();
			return factoryBean.getObject();
		}

		@Bean
		public Job job(JobRepository jobRepository, Step step) {
			return new JobBuilder(jobRepository).start(step).build();
		}

		@Bean
		public Step step(JobRepository jobRepository, JpaTransactionManager transactionManager,
				EntityManagerFactory entityManagerFactory, JdbcTemplate jdbcTemplate) {
			AtomicInteger writeInvocations = new AtomicInteger();
			ItemWriter<Integer> writer = chunk -> {
				EntityManager entityManager = EntityManagerFactoryUtils
					.getTransactionalEntityManager(entityManagerFactory);
				for (Integer id : chunk) {
					entityManager.find(PersonEntity.class, id).setName("v2");
				}
				if (writeInvocations.incrementAndGet() == 1) {
					// simulate a concurrent process bumping the row's version between
					// this writer's read and its flush
					jdbcTemplate.update("update person_target set version = version + 1 where id = 1");
				}
				// attempt 1: flush throws OptimisticLockException and marks the
				// transaction rollback-only
				// attempt 2 (in-place retry, same transaction): the entity is no longer
				// dirty, so this does not throw, even though the transaction is
				// already doomed
				entityManager.flush();
			};
			return new ChunkOrientedStepBuilder<Integer, Integer>(jobRepository, 10)
				.reader(new ListItemReader<>(List.of(1)))
				.writer(writer)
				.transactionManager(transactionManager)
				.faultTolerant()
				.retry(OptimisticLockException.class)
				.retryLimit(3)
				.build();
		}

	}

	@Configuration
	@EnableBatchProcessing
	@EnableJdbcJobRepository
	@Import(JpaInfrastructureConfiguration.class)
	static class JobConfiguration {

		@Bean
		public Job job(JobRepository jobRepository, Step step) {
			return new JobBuilder(jobRepository).start(step).build();
		}

		@Bean
		public Step step(JobRepository jobRepository, JpaTransactionManager transactionManager,
				EntityManagerFactory entityManagerFactory) {
			// @formatter:off
			List<Person> items = List.of(
					new Person(1, "foo1"),
					new Person(2, "foooo2"), // this item will cause a write failure due to the name length constraint
					new Person(3, "foo3"),
					new Person(4, "foo4"),
					new Person(5, "foooo5"), // this item will cause a write failure due to the name length constraint
					new Person(6, "foo6"));
			// @formatter:on
			return new ChunkOrientedStepBuilder<Person, PersonEntity>(jobRepository, 3)
				.reader(new ListItemReader<>(items))
				.processor(item -> new PersonEntity(item.id(), item.name()))
				.writer(new JpaItemWriter<>(entityManagerFactory))
				.transactionManager(transactionManager)
				.faultTolerant()
				.skipPolicy(new AlwaysSkipItemSkipPolicy())
				.skipLimit(10)
				.build();
		}

	}

}