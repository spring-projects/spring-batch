/*
 * Copyright 2013-2025 the original author or authors.
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.JobInterruptedException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.job.UnexpectedJobExecutionException;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.configuration.xml.DummyStep;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.FlowStep;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.job.flow.support.StateTransition;
import org.springframework.batch.core.job.flow.support.state.EndState;
import org.springframework.batch.core.job.flow.support.state.StepState;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration tests for the <code>SimpleJobExplorer</code> implementation.
 *
 * @author Sergey Shcherbakov
 * @author Mahmoud Ben Hassine
 */
@SpringJUnitConfig(classes = { SimpleJobExplorerIntegrationTests.Config.class })
class SimpleJobExplorerIntegrationTests {

	/*
	 * Integration test for the BATCH-2034 issue. The {@link FlowStep} execution should
	 * not fail in the remote partitioning use case because the {@link SimpleJobExplorer}
	 * doesn't retrieve the {@link JobInstance} from the {@link JobRepository}. To
	 * illustrate the issue the test simulates the behavior of the {@code
	 * StepExecutionRequestHandler} from the spring-batch-integration project.
	 */
	@Configuration
	@EnableBatchProcessing
	@EnableJdbcJobRepository
	static class Config {

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
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
				.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
				.generateUniqueName(true)
				.build();
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
	private FlowStep flowStep;

	@Autowired
	private JobOperator jobOperator;

	@Autowired
	private Job job;

	@Test
	void testGetStepExecution() throws JobExecutionAlreadyRunningException, JobRestartException,
			JobInstanceAlreadyCompleteException, JobInterruptedException, UnexpectedJobExecutionException {

		// Prepare the jobRepository for the test
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance("myJob", jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());
		StepExecution stepExecution = jobRepository.createStepExecution("flowStep", jobExecution);

		// Executed on the remote end in remote partitioning use case
		StepExecution jobExplorerStepExecution = jobRepository.getStepExecution(jobExecution.getId(),
				stepExecution.getId());
		flowStep.execute(jobExplorerStepExecution);

		assertEquals(BatchStatus.COMPLETED, jobExplorerStepExecution.getStatus());
	}

	@Test
	void getLastJobExecutionShouldFetchStepExecutions() throws Exception {
		this.jobOperator.start(this.job, new JobParameters());
		JobInstance lastJobInstance = this.jobRepository.getLastJobInstance("job");
		JobExecution lastJobExecution = this.jobRepository.getLastJobExecution(lastJobInstance);
		assertEquals(1, lastJobExecution.getStepExecutions().size());
		StepExecution stepExecution = lastJobExecution.getStepExecutions().iterator().next();
		assertNotNull(stepExecution.getExecutionContext());
	}

	/*
	 * Test case for https://github.com/spring-projects/spring-batch/issues/4246:
	 * SimpleJobExplorer#getJobExecutions(JobInstance) should return a list of job
	 * executions, where each execution has its own job parameters.
	 */

	@Configuration
	@EnableBatchProcessing
	@EnableJdbcJobRepository
	static class JobConfiguration {

		@Bean
		public Step step(JobRepository jobRepository, JdbcTransactionManager transactionManager) {
			return new StepBuilder("step", jobRepository).tasklet((contribution, chunkContext) -> {
				throw new RuntimeException("Expected failure!");
			}, transactionManager).build();
		}

		@Bean
		public Job job(JobRepository jobRepository, Step step) {
			return new JobBuilder("job", jobRepository).start(step).build();
		}

		@Bean
		public DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
				.addScript("/org/springframework/batch/core/schema-h2.sql")
				.generateUniqueName(true)
				.build();
		}

		@Bean
		public JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

	}

	@Test
	void retrievedJobExecutionsShouldHaveTheirOwnParameters() throws Exception {
		// given
		ApplicationContext context = new AnnotationConfigApplicationContext(JobConfiguration.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);
		JobRepository jobRepository = context.getBean(JobRepository.class);
		Job job = context.getBean(Job.class);
		long id = 1L;
		JobParameters jobParameters1 = new JobParametersBuilder().addLong("id", id)
			.addString("name", "foo", false)
			.toJobParameters();
		JobParameters jobParameters2 = new JobParametersBuilder().addLong("id", id)
			.addString("name", "bar", false)
			.toJobParameters();

		// when
		JobExecution jobExecution1 = jobOperator.start(job, jobParameters1);
		JobExecution jobExecution2 = jobOperator.start(job, jobParameters2);

		// then
		Assertions.assertEquals(jobExecution1.getJobInstance(), jobExecution2.getJobInstance());
		List<JobExecution> jobExecutions = jobRepository.getJobExecutions(jobExecution1.getJobInstance());
		Assertions.assertEquals(2, jobExecutions.size());
		JobParameters actualJobParameters1 = jobExecutions.get(0).getJobParameters();
		JobParameters actualJobParameters2 = jobExecutions.get(1).getJobParameters();
		Assertions.assertEquals(actualJobParameters1.getParameter("id"), actualJobParameters2.getParameter("id"));
		Assertions.assertEquals(actualJobParameters1.getParameter("name"), actualJobParameters2.getParameter("name"));
	}

}
