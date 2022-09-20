/*
 * Copyright 2013-2022 the original author or authors.
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
package org.springframework.batch.core.explore.support;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.Test;
import test.jdbc.datasource.DataSourceInitializer;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.xml.DummyStep;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.FlowStep;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.job.flow.support.StateTransition;
import org.springframework.batch.core.job.flow.support.state.EndState;
import org.springframework.batch.core.job.flow.support.state.StepState;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration test for the BATCH-2034 issue. The {@link FlowStep} execution should not
 * fail in the remote partitioning use case because the {@link SimpleJobExplorer} doesn't
 * retrieve the {@link JobInstance} from the {@link JobRepository}. To illustrate the
 * issue the test simulates the behavior of the {@code StepExecutionRequestHandler} from
 * the spring-batch-integration project.
 *
 * @author Sergey Shcherbakov
 * @author Mahmoud Ben Hassine
 */
@SpringJUnitConfig(classes = { SimpleJobExplorerIntegrationTests.Config.class })
class SimpleJobExplorerIntegrationTests {

	@Configuration
	@EnableBatchProcessing
	static class Config {

		@Bean
		public JobExplorer jobExplorer() throws Exception {
			return jobExplorerFactoryBean().getObject();
		}

		@Bean
		public JobExplorerFactoryBean jobExplorerFactoryBean() {
			JobExplorerFactoryBean jobExplorerFactoryBean = new JobExplorerFactoryBean();
			jobExplorerFactoryBean.setDataSource(dataSource());
			return jobExplorerFactoryBean;
		}

		@Bean
		public Step flowStep(JobRepository jobRepository) {
			return new StepBuilder("flowStep", jobRepository).flow(simpleFlow()).build();
		}

		@Bean
		public Step dummyStep() {
			return new DummyStep();
		}

		@Bean
		public SimpleFlow simpleFlow() {
			SimpleFlow simpleFlow = new SimpleFlow("simpleFlow");
			List<StateTransition> transitions = new ArrayList<>();
			transitions.add(StateTransition.createStateTransition(new StepState(dummyStep()), "end0"));
			transitions
					.add(StateTransition.createEndStateTransition(new EndState(FlowExecutionStatus.COMPLETED, "end0")));
			simpleFlow.setStateTransitions(transitions);
			return simpleFlow;
		}

		@Bean
		public BasicDataSource dataSource() {
			BasicDataSource dataSource = new BasicDataSource();
			dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
			dataSource.setUrl("jdbc:hsqldb:mem:testdb;sql.enforce_strict_size=true;hsqldb.tx=mvcc");
			dataSource.setUsername("sa");
			dataSource.setPassword("");
			return dataSource;
		}

		@Bean
		public DataSourceInitializer dataSourceInitializer() {
			DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
			dataSourceInitializer.setDataSource(dataSource());
			dataSourceInitializer.setInitScripts(
					new Resource[] { new ClassPathResource("org/springframework/batch/core/schema-drop-hsqldb.sql"),
							new ClassPathResource("org/springframework/batch/core/schema-hsqldb.sql") });
			return dataSourceInitializer;
		}

		@Bean
		public JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

		@Bean
		public Job job(JobRepository jobRepository) {
			return new JobBuilder("job", jobRepository).start(dummyStep()).build();
		}

	}

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private JobExplorer jobExplorer;

	@Autowired
	private FlowStep flowStep;

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private Job job;

	@Test
	void testGetStepExecution() throws JobExecutionAlreadyRunningException, JobRestartException,
			JobInstanceAlreadyCompleteException, JobInterruptedException, UnexpectedJobExecutionException {

		// Prepare the jobRepository for the test
		JobExecution jobExecution = jobRepository.createJobExecution("myJob", new JobParameters());
		StepExecution stepExecution = jobExecution.createStepExecution("flowStep");
		jobRepository.add(stepExecution);

		// Executed on the remote end in remote partitioning use case
		StepExecution jobExplorerStepExecution = jobExplorer.getStepExecution(jobExecution.getId(),
				stepExecution.getId());
		flowStep.execute(jobExplorerStepExecution);

		assertEquals(BatchStatus.COMPLETED, jobExplorerStepExecution.getStatus());
	}

	@Test
	void getLastJobExecutionShouldFetchStepExecutions() throws Exception {
		this.jobLauncher.run(this.job, new JobParameters());
		JobInstance lastJobInstance = this.jobExplorer.getLastJobInstance("job");
		JobExecution lastJobExecution = this.jobExplorer.getLastJobExecution(lastJobInstance);
		assertEquals(1, lastJobExecution.getStepExecutions().size());
		StepExecution stepExecution = lastJobExecution.getStepExecutions().iterator().next();
		assertNotNull(stepExecution.getExecutionContext());
	}

}
