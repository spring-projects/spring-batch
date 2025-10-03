/*
 * Copyright 2006-2025 the original author or authors.
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
package org.springframework.batch.core.partition;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.partition.support.DefaultStepExecutionAggregator;
import org.springframework.batch.core.partition.support.SimplePartitioner;
import org.springframework.batch.core.partition.support.SimpleStepExecutionSplitter;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class PartitionStepTests {

	private PartitionStep step;

	private JobRepository jobRepository;

	@BeforeEach
	void setUp() throws Exception {
		EmbeddedDatabase embeddedDatabase = new EmbeddedDatabaseBuilder()
			.addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
			.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
			.build();
		JdbcJobRepositoryFactoryBean factory = new JdbcJobRepositoryFactoryBean();
		factory.setDataSource(embeddedDatabase);
		factory.setTransactionManager(new JdbcTransactionManager(embeddedDatabase));
		factory.afterPropertiesSet();
		jobRepository = factory.getObject();
		step = new PartitionStep(jobRepository);
		step.setName("partitioned");
	}

	@Test
	void testVanillaStepExecution() throws Exception {
		SimpleStepExecutionSplitter stepExecutionSplitter = new SimpleStepExecutionSplitter(jobRepository,
				step.getName(), new SimplePartitioner());
		stepExecutionSplitter.setAllowStartIfComplete(true);
		step.setStepExecutionSplitter(stepExecutionSplitter);
		step.setPartitionHandler((stepSplitter, stepExecution) -> {
			Set<StepExecution> executions = stepSplitter.split(stepExecution, 2);
			for (StepExecution execution : executions) {
				execution.setStatus(BatchStatus.COMPLETED);
				execution.setExitStatus(ExitStatus.COMPLETED);
				jobRepository.update(execution);
				jobRepository.updateExecutionContext(execution);
			}
			return executions;
		});
		step.afterPropertiesSet();
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance("vanillaJob", jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());
		StepExecution stepExecution = jobRepository.createStepExecution("foo", jobExecution);
		step.execute(stepExecution);
		// one manager and two workers
		assertEquals(3, stepExecution.getJobExecution().getStepExecutions().size());
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
	}

	@Test
	void testFailedStepExecution() throws Exception {
		SimpleStepExecutionSplitter stepExecutionSplitter = new SimpleStepExecutionSplitter(jobRepository,
				step.getName(), new SimplePartitioner());
		stepExecutionSplitter.setAllowStartIfComplete(true);
		step.setStepExecutionSplitter(stepExecutionSplitter);
		step.setPartitionHandler((stepSplitter, stepExecution) -> {
			Set<StepExecution> executions = stepSplitter.split(stepExecution, 2);
			for (StepExecution execution : executions) {
				execution.setStatus(BatchStatus.FAILED);
				execution.setExitStatus(ExitStatus.FAILED);
				jobRepository.update(execution);
				jobRepository.updateExecutionContext(execution);
			}
			return executions;
		});
		step.afterPropertiesSet();
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance("vanillaJob", jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());
		StepExecution stepExecution = jobRepository.createStepExecution("foo", jobExecution);
		step.execute(stepExecution);
		// one manager and two workers
		assertEquals(3, stepExecution.getJobExecution().getStepExecutions().size());
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
	}

	@Test
	void testRestartStepExecution() throws Exception {
		final AtomicBoolean started = new AtomicBoolean(false);
		SimpleStepExecutionSplitter stepExecutionSplitter = new SimpleStepExecutionSplitter(jobRepository,
				step.getName(), new SimplePartitioner());
		stepExecutionSplitter.setAllowStartIfComplete(true);
		step.setStepExecutionSplitter(stepExecutionSplitter);
		step.setPartitionHandler((stepSplitter, stepExecution) -> {
			Set<StepExecution> executions = stepSplitter.split(stepExecution, 2);
			if (!started.get()) {
				started.set(true);
				for (StepExecution execution : executions) {
					execution.setStatus(BatchStatus.FAILED);
					execution.setExitStatus(ExitStatus.FAILED);
					execution.getExecutionContext().putString("foo", execution.getStepName());
				}
			}
			else {
				for (StepExecution execution : executions) {
					// On restart the execution context should have been restored
					assertEquals(execution.getStepName(), execution.getExecutionContext().getString("foo"));
				}
			}
			for (StepExecution execution : executions) {
				jobRepository.update(execution);
				jobRepository.updateExecutionContext(execution);
			}
			return executions;
		});
		step.afterPropertiesSet();
		JobParameters jobParameters = new JobParameters();
		ExecutionContext executionContext = new ExecutionContext();
		JobInstance jobInstance = jobRepository.createJobInstance("vanillaJob", jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters, executionContext);
		StepExecution stepExecution = jobRepository.createStepExecution("foo", jobExecution);
		step.execute(stepExecution);
		jobExecution.setStatus(BatchStatus.FAILED);
		jobExecution.setEndTime(LocalDateTime.now());
		jobRepository.update(jobExecution);
		// one manager and two workers
		assertEquals(3, jobExecution.getStepExecutions().size());
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());

		// Now restart...
		JobExecution jobExecution2 = jobRepository.createJobExecution(jobInstance, jobParameters, executionContext);
		StepExecution stepExecution2 = jobRepository.createStepExecution("foo", jobExecution2);
		step.execute(stepExecution2);
		// one manager and two workers
		assertEquals(3, jobExecution2.getStepExecutions().size());
		assertEquals(BatchStatus.COMPLETED, stepExecution2.getStatus());
	}

	@Test
	void testStoppedStepExecution() throws Exception {
		SimpleStepExecutionSplitter stepExecutionSplitter = new SimpleStepExecutionSplitter(jobRepository,
				step.getName(), new SimplePartitioner());
		stepExecutionSplitter.setAllowStartIfComplete(true);
		step.setStepExecutionSplitter(stepExecutionSplitter);
		step.setPartitionHandler((stepSplitter, stepExecution) -> {
			Set<StepExecution> executions = stepSplitter.split(stepExecution, 2);
			for (StepExecution execution : executions) {
				execution.setStatus(BatchStatus.STOPPED);
				execution.setExitStatus(ExitStatus.STOPPED);
			}
			return executions;
		});
		step.afterPropertiesSet();
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance("vanillaJob", jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());
		StepExecution stepExecution = jobRepository.createStepExecution("foo", jobExecution);
		step.execute(stepExecution);
		// one manager and two workers
		assertEquals(3, stepExecution.getJobExecution().getStepExecutions().size());
		assertEquals(BatchStatus.STOPPED, stepExecution.getStatus());
	}

	@Test
	void testStepAggregator() throws Exception {
		step.setStepExecutionAggregator(new DefaultStepExecutionAggregator() {
			@Override
			public void aggregate(StepExecution result, Collection<StepExecution> executions) {
				super.aggregate(result, executions);
				result.getExecutionContext().put("aggregated", true);
			}
		});
		SimpleStepExecutionSplitter stepExecutionSplitter = new SimpleStepExecutionSplitter(jobRepository,
				step.getName(), new SimplePartitioner());
		stepExecutionSplitter.setAllowStartIfComplete(true);
		step.setStepExecutionSplitter(stepExecutionSplitter);
		step.setPartitionHandler((stepSplitter, stepExecution) -> Arrays.asList(stepExecution));
		step.afterPropertiesSet();
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance("vanillaJob", jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());
		StepExecution stepExecution = jobRepository.createStepExecution("foo", jobExecution);
		step.execute(stepExecution);
		assertEquals(true, stepExecution.getExecutionContext().get("aggregated"));
	}

}
