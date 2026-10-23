/*
 * Copyright 2006-present the original author or authors.
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

import org.springframework.batch.infrastructure.support.DatabaseType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @author Yanming Zhou
 *
 */
class PartitionStepTests {

	private PartitionStep step;

	private JobRepository jobRepository;

	@BeforeEach
	void setUp() throws Exception {
		EmbeddedDatabase embeddedDatabase = new EmbeddedDatabaseBuilder()
			.addScript(DatabaseType.HSQL.getProductSchemaDrop())
			.addScript(DatabaseType.HSQL.getProductSchema())
			.generateUniqueName(true)
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
				step.getName(), gridSize -> {
					Map<String, ExecutionContext> map = new HashMap<>(gridSize);
					for (int i = 0; i < gridSize; i++) {
						ExecutionContext context = new ExecutionContext();
						context.putString("foo", "foo" + i);
						map.put("partition" + i, context);
					}
					return map;
				});
		stepExecutionSplitter.setAllowStartIfComplete(true);
		step.setStepExecutionSplitter(stepExecutionSplitter);
		step.setPartitionHandler((stepSplitter, stepExecution) -> {
			Set<StepExecution> executions = stepSplitter.split(stepExecution, 2);
			for (StepExecution execution : executions) {
				// Query from repository to ensure it's persisted
				ExecutionContext context = jobRepository.getStepExecution(execution.getId()).getExecutionContext();
				assertNotNull(context.getString("foo"));

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
					// Query from repository to ensure it's persisted
					ExecutionContext context = jobRepository.getStepExecution(execution.getId()).getExecutionContext();
					assertEquals(execution.getStepName(), context.getString("foo"));
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

	/**
	 * The partitions that already completed before a restart are not re-executed, but
	 * they must still be part of the aggregated result of the restarted manager step.
	 */
	@Test
	void testRestartWhenAllPartitionsAlreadyCompleted() throws Exception {
		List<Collection<StepExecution>> aggregated = new ArrayList<>();
		step.setStepExecutionAggregator(new DefaultStepExecutionAggregator() {
			@Override
			public void aggregate(StepExecution result, Collection<StepExecution> executions) {
				aggregated.add(new ArrayList<>(executions));
				super.aggregate(result, executions);
			}
		});
		SimpleStepExecutionSplitter stepExecutionSplitter = new SimpleStepExecutionSplitter(jobRepository,
				step.getName(), new SimplePartitioner());
		step.setStepExecutionSplitter(stepExecutionSplitter);
		AtomicBoolean managerCrashes = new AtomicBoolean(true);
		step.setPartitionHandler((stepSplitter, managerStepExecution) -> {
			Set<StepExecution> executions = stepSplitter.split(managerStepExecution, 2);
			for (StepExecution execution : executions) {
				execution.setStatus(BatchStatus.COMPLETED);
				execution.setExitStatus(ExitStatus.COMPLETED);
				execution.setWriteCount(3);
				jobRepository.update(execution);
			}
			if (managerCrashes.get()) {
				// the manager dies after the workers completed
				throw new IllegalStateException("manager crashed");
			}
			return executions;
		});
		step.afterPropertiesSet();

		JobParameters jobParameters = new JobParameters();
		ExecutionContext executionContext = new ExecutionContext();
		JobInstance jobInstance = jobRepository.createJobInstance("restartJob", jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters, executionContext);
		StepExecution stepExecution = jobRepository.createStepExecution("foo", jobExecution);
		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		jobExecution.setStatus(BatchStatus.FAILED);
		jobExecution.setEndTime(LocalDateTime.now());
		jobRepository.update(jobExecution);

		// Now restart: both partitions already completed, so none is re-executed
		managerCrashes.set(false);
		JobExecution jobExecution2 = jobRepository.createJobExecution(jobInstance, jobParameters, executionContext);
		StepExecution stepExecution2 = jobRepository.createStepExecution("foo", jobExecution2);
		step.execute(stepExecution2);

		assertEquals(1, aggregated.size());
		assertEquals(2, aggregated.get(0).size());
		assertEquals(6, stepExecution2.getWriteCount());
		assertEquals(BatchStatus.COMPLETED, stepExecution2.getStatus());
	}

	/**
	 * Only the partitions that did not complete are re-executed on a restart, but the
	 * aggregated result must cover all partitions.
	 */
	@Test
	void testRestartWhenSomePartitionsAlreadyCompleted() throws Exception {
		List<Collection<StepExecution>> aggregated = new ArrayList<>();
		step.setStepExecutionAggregator(new DefaultStepExecutionAggregator() {
			@Override
			public void aggregate(StepExecution result, Collection<StepExecution> executions) {
				aggregated.add(new ArrayList<>(executions));
				super.aggregate(result, executions);
			}
		});
		SimpleStepExecutionSplitter stepExecutionSplitter = new SimpleStepExecutionSplitter(jobRepository,
				step.getName(), new SimplePartitioner());
		step.setStepExecutionSplitter(stepExecutionSplitter);
		AtomicBoolean firstRun = new AtomicBoolean(true);
		step.setPartitionHandler((stepSplitter, managerStepExecution) -> {
			Set<StepExecution> executions = stepSplitter.split(managerStepExecution, 2);
			for (StepExecution execution : executions) {
				boolean failing = firstRun.get() && execution.getStepName().endsWith("partition1");
				execution.setStatus(failing ? BatchStatus.FAILED : BatchStatus.COMPLETED);
				execution.setExitStatus(failing ? ExitStatus.FAILED : ExitStatus.COMPLETED);
				execution.setWriteCount(3);
				jobRepository.update(execution);
			}
			firstRun.set(false);
			return executions;
		});
		step.afterPropertiesSet();

		JobParameters jobParameters = new JobParameters();
		ExecutionContext executionContext = new ExecutionContext();
		JobInstance jobInstance = jobRepository.createJobInstance("restartJob", jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters, executionContext);
		StepExecution stepExecution = jobRepository.createStepExecution("foo", jobExecution);
		step.execute(stepExecution);
		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(2, aggregated.get(0).size());
		jobExecution.setStatus(BatchStatus.FAILED);
		jobExecution.setEndTime(LocalDateTime.now());
		jobRepository.update(jobExecution);

		// Now restart: only the failed partition is re-executed
		JobExecution jobExecution2 = jobRepository.createJobExecution(jobInstance, jobParameters, executionContext);
		StepExecution stepExecution2 = jobRepository.createStepExecution("foo", jobExecution2);
		step.execute(stepExecution2);

		assertEquals(1, jobExecution2.getStepExecutions().size() - 1);
		assertEquals(2, aggregated.get(1).size());
		assertEquals(6, stepExecution2.getWriteCount());
		assertEquals(BatchStatus.COMPLETED, stepExecution2.getStatus());
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
