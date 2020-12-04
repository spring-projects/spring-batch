/*
 * Copyright 2015-2019 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.datasource.embedded.ConnectionProperties;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseConfigurer;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.lang.Nullable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ClassUtils;

import static org.junit.Assert.assertEquals;

/**
 * @author Michael Minella
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ConcurrentTransactionTests.ConcurrentJobConfiguration.class)
public class ConcurrentTransactionTests {

	@Autowired
	private Job concurrentJob;

	@Autowired
	private JobLauncher jobLauncher;

	@DirtiesContext
	@Test
	public void testConcurrentLongRunningJobExecutions() throws Exception {

		JobExecution jobExecution = jobLauncher.run(concurrentJob, new JobParameters());

		assertEquals(jobExecution.getStatus(), BatchStatus.COMPLETED);
	}

	@Configuration
	@EnableBatchProcessing
	public static class ConcurrentJobConfiguration extends DefaultBatchConfigurer {

		@Autowired
		private JobBuilderFactory jobBuilderFactory;

		@Autowired
		private StepBuilderFactory stepBuilderFactory;

		@Bean
		public TaskExecutor taskExecutor() {
			return new SimpleAsyncTaskExecutor();
		}

		/**
		 * This datasource configuration configures the HSQLDB instance using MVCC.  When
		 * configured using the default behavior, transaction serialization errors are
		 * thrown (default configuration example below).
		 *
		 * 			return new PooledEmbeddedDataSource(new EmbeddedDatabaseBuilder().
		 * 				 addScript("classpath:org/springframework/batch/core/schema-drop-hsqldb.sql").
		 * 				 addScript("classpath:org/springframework/batch/core/schema-hsqldb.sql").
		 * 				 build());

		 * @return
		 */
		@Bean
		DataSource dataSource() {
			ResourceLoader defaultResourceLoader = new DefaultResourceLoader();
			EmbeddedDatabaseFactory embeddedDatabaseFactory = new EmbeddedDatabaseFactory();
			embeddedDatabaseFactory.setDatabaseConfigurer(new EmbeddedDatabaseConfigurer() {

				@Override
				@SuppressWarnings("unchecked")
				public void configureConnectionProperties(ConnectionProperties properties, String databaseName) {
					try {
						properties.setDriverClass((Class<? extends Driver>) ClassUtils.forName("org.hsqldb.jdbcDriver", this.getClass().getClassLoader()));
					}
					catch (Exception e) {
						e.printStackTrace();
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
			databasePopulator.addScript(defaultResourceLoader.getResource("classpath:org/springframework/batch/core/schema-drop-hsqldb.sql"));
			databasePopulator.addScript(defaultResourceLoader.getResource("classpath:org/springframework/batch/core/schema-hsqldb.sql"));
			embeddedDatabaseFactory.setDatabasePopulator(databasePopulator);

			return embeddedDatabaseFactory.getDatabase();
		}

		@Bean
		public Flow flow() {
			return new FlowBuilder<Flow>("flow")
					.start(stepBuilderFactory.get("flow.step1")
								.tasklet(new Tasklet() {
									@Nullable
									@Override
									public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
										return RepeatStatus.FINISHED;
									}
								}).build()
					).next(stepBuilderFactory.get("flow.step2")
								.tasklet(new Tasklet() {
									@Nullable
									@Override
									public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
										return RepeatStatus.FINISHED;
									}
								}).build()
					).build();
		}

		@Bean
		public Step firstStep() {
			return stepBuilderFactory.get("firstStep")
					.tasklet(new Tasklet() {
						@Nullable
						@Override
						public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
							System.out.println(">> Beginning concurrent job test");
							return RepeatStatus.FINISHED;
						}
					}).build();
		}

		@Bean
		public Step lastStep() {
			return stepBuilderFactory.get("lastStep")
					.tasklet(new Tasklet() {
						@Nullable
						@Override
						public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
							System.out.println(">> Ending concurrent job test");
							return RepeatStatus.FINISHED;
						}
					}).build();
		}

		@Bean
		public Job concurrentJob() {
			Flow splitFlow = new FlowBuilder<Flow>("splitflow").split(new SimpleAsyncTaskExecutor()).add(flow(), flow(), flow(), flow(), flow(), flow(), flow()).build();

			return jobBuilderFactory.get("concurrentJob")
					.start(firstStep())
					.next(stepBuilderFactory.get("splitFlowStep")
							.flow(splitFlow)
							.build())
					.next(lastStep())
					.build();
		}

		@Override
		protected JobRepository createJobRepository() throws Exception {
			JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
			factory.setDataSource(dataSource());
			factory.setIsolationLevelForCreate("ISOLATION_READ_COMMITTED");
			factory.setTransactionManager(getTransactionManager());
			factory.afterPropertiesSet();
			return  factory.getObject();
		}
	}
}
