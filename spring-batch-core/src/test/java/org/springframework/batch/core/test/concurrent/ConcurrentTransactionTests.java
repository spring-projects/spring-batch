/*
 * Copyright 2015-2023 the original author or authors.
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
package org.springframework.batch.core.test.concurrent;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.datasource.embedded.ConnectionProperties;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseConfigurer;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.lang.Nullable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.util.ClassUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 */
// FIXME incorrect configuration of JobLauncher. This should be failing with v4.
@Disabled
@SpringJUnitConfig(classes = ConcurrentTransactionTests.ConcurrentJobConfiguration.class)
class ConcurrentTransactionTests {

	@Autowired
	private Job concurrentJob;

	@Autowired
	private JobLauncher jobLauncher;

	@DirtiesContext
	@Test
	void testConcurrentLongRunningJobExecutions() throws Exception {

		JobExecution jobExecution = jobLauncher.run(concurrentJob, new JobParameters());

		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
	}

	@Configuration
	@EnableBatchProcessing
	@Import(DataSourceConfiguration.class)
	public static class ConcurrentJobConfiguration {

		@Bean
		public TaskExecutor taskExecutor() {
			return new SimpleAsyncTaskExecutor();
		}

		@Bean
		public Flow flow(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
			return new FlowBuilder<Flow>("flow")
				.start(new StepBuilder("flow.step1", jobRepository).tasklet(new Tasklet() {
					@Nullable
					@Override
					public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
							throws Exception {
						return RepeatStatus.FINISHED;
					}
				}, transactionManager).build())
				.next(new StepBuilder("flow.step2", jobRepository).tasklet(new Tasklet() {
					@Nullable
					@Override
					public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
							throws Exception {
						return RepeatStatus.FINISHED;
					}
				}, transactionManager).build())
				.build();
		}

		@Bean
		public Step firstStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
			return new StepBuilder("firstStep", jobRepository).tasklet(new Tasklet() {
				@Nullable
				@Override
				public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
					return RepeatStatus.FINISHED;
				}
			}, transactionManager).build();
		}

		@Bean
		public Step lastStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
			return new StepBuilder("lastStep", jobRepository).tasklet(new Tasklet() {
				@Nullable
				@Override
				public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
					return RepeatStatus.FINISHED;
				}
			}, transactionManager).build();
		}

		@Bean
		public Job concurrentJob(JobRepository jobRepository, PlatformTransactionManager transactionManager,
				TaskExecutor taskExecutor) {
			Flow splitFlow = new FlowBuilder<Flow>("splitflow").split(taskExecutor)
				.add(flow(jobRepository, transactionManager), flow(jobRepository, transactionManager),
						flow(jobRepository, transactionManager), flow(jobRepository, transactionManager),
						flow(jobRepository, transactionManager), flow(jobRepository, transactionManager),
						flow(jobRepository, transactionManager))
				.build();

			return new JobBuilder("concurrentJob", jobRepository).start(firstStep(jobRepository, transactionManager))
				.next(new StepBuilder("splitFlowStep", jobRepository).flow(splitFlow).build())
				.next(lastStep(jobRepository, transactionManager))
				.build();
		}

		@Bean
		public JobRepository jobRepository(DataSource dataSource, PlatformTransactionManager transactionManager)
				throws Exception {
			JdbcJobRepositoryFactoryBean factory = new JdbcJobRepositoryFactoryBean();
			factory.setDataSource(dataSource);
			factory.setIsolationLevelForCreateEnum(Isolation.READ_COMMITTED);
			factory.setTransactionManager(transactionManager);
			factory.afterPropertiesSet();
			return factory.getObject();
		}

	}

	@Configuration
	static class DataSourceConfiguration {

		/*
		 * This datasource configuration configures the HSQLDB instance using MVCC. When
		 * configured using the default behavior, transaction serialization errors are
		 * thrown (default configuration example below).
		 *
		 * return new PooledEmbeddedDataSource(new EmbeddedDatabaseBuilder().
		 * addScript("classpath:org/springframework/batch/core/schema-drop-hsqldb.sql").
		 * addScript("classpath:org/springframework/batch/core/schema-hsqldb.sql").
		 * build());
		 */
		@Bean
		public DataSource dataSource() {
			ResourceLoader defaultResourceLoader = new DefaultResourceLoader();
			EmbeddedDatabaseFactory embeddedDatabaseFactory = new EmbeddedDatabaseFactory();
			embeddedDatabaseFactory.setDatabaseConfigurer(new EmbeddedDatabaseConfigurer() {

				@Override
				@SuppressWarnings("unchecked")
				public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
					try {
						properties.setDriverClass((Class<? extends Driver>) ClassUtils.forName("org.hsqldb.jdbcDriver",
								this.getClass().getClassLoader()));
					}
					catch (Exception e) {
					}
					properties.setUrl("jdbc:hsqldb:mem:" + databaseName + ";hsqldb.tx=mvcc");
					properties.setUsername("sa");
					properties.setPassword("");
				}

				@Override
				public void shutdown(DataSource dataSource, String databaseName) {
					try {
						Connection connection = dataSource.getConnection();
						Statement stmt = connection.createStatement();
						stmt.execute("SHUTDOWN");
					}
					catch (SQLException ex) {
					}
				}
			});

			ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
			databasePopulator.addScript(defaultResourceLoader
				.getResource("classpath:org/springframework/batch/core/schema-drop-hsqldb.sql"));
			databasePopulator.addScript(
					defaultResourceLoader.getResource("classpath:org/springframework/batch/core/schema-hsqldb.sql"));
			embeddedDatabaseFactory.setDatabasePopulator(databasePopulator);
			embeddedDatabaseFactory.setGenerateUniqueDatabaseName(true);

			return embeddedDatabaseFactory.getDatabase();
		}

		@Bean
		public JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

	}

}
