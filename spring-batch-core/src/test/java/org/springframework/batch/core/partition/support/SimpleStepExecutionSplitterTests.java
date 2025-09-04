/*
 * Copyright 2008-2025 the original author or authors.
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
package org.springframework.batch.core.partition.support;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobExecutionException;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.partition.PartitionNameProvider;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleStepExecutionSplitterTests {

	private Step step;

	private JobRepository jobRepository;

	private StepExecution stepExecution;

	@BeforeEach
	void setUp() throws Exception {
		step = new TaskletStep("step");
		EmbeddedDatabase embeddedDatabase = new EmbeddedDatabaseBuilder()
			.addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
			.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
			.build();
		JdbcJobRepositoryFactoryBean factory = new JdbcJobRepositoryFactoryBean();
		factory.setDataSource(embeddedDatabase);
		factory.setTransactionManager(new JdbcTransactionManager(embeddedDatabase));
		factory.afterPropertiesSet();
		jobRepository = factory.getObject();
		stepExecution = jobRepository.createJobExecution("job", new JobParameters()).createStepExecution("bar");
		jobRepository.add(stepExecution);
	}

	@Test
	void testSimpleStepExecutionProviderJobRepositoryStep() throws Exception {
		SimpleStepExecutionSplitter splitter = new SimpleStepExecutionSplitter(jobRepository, true, step.getName(),
				new SimplePartitioner());
		Set<StepExecution> execs = splitter.split(stepExecution, 2);
		assertEquals(2, execs.size());

		for (StepExecution execution : execs) {
			assertNotNull(execution.getId(), "step execution partition is saved");
		}
	}

	/*
	 * Tests the results of BATCH-2490
	 */
	@Test
	void testAddressabilityOfSetResults() throws Exception {
		SimpleStepExecutionSplitter splitter = new SimpleStepExecutionSplitter(jobRepository, true, step.getName(),
				new SimplePartitioner());
		Set<StepExecution> execs = splitter.split(stepExecution, 2);
		assertEquals(2, execs.size());

		StepExecution execution = execs.iterator().next();
		execs.remove(execution);
		assertEquals(1, execs.size());
	}

	@Test
	void testSimpleStepExecutionProviderJobRepositoryStepPartitioner() throws Exception {
		final Map<String, ExecutionContext> map = Collections.singletonMap("foo", new ExecutionContext());
		SimpleStepExecutionSplitter splitter = new SimpleStepExecutionSplitter(jobRepository, true, step.getName(),
				gridSize -> map);
		assertEquals(1, splitter.split(stepExecution, 2).size());
	}

	@Test
	void testRememberGridSize() throws Exception {
		SimpleStepExecutionSplitter provider = new SimpleStepExecutionSplitter(jobRepository, true, step.getName(),
				new SimplePartitioner());
		Set<StepExecution> split = provider.split(stepExecution, 2);
		assertEquals(2, split.size());
		stepExecution = update(split, stepExecution, BatchStatus.FAILED);
		assertEquals(2, provider.split(stepExecution, 3).size());
	}

	@Test
	void testRememberPartitionNames() throws Exception {
		class CustomPartitioner implements Partitioner, PartitionNameProvider {

			@Override
			public Map<String, ExecutionContext> partition(int gridSize) {
				return Collections.singletonMap("foo", new ExecutionContext());
			}

			@Override
			public Collection<String> getPartitionNames(int gridSize) {
				return Arrays.asList("foo");
			}

		}
		SimpleStepExecutionSplitter provider = new SimpleStepExecutionSplitter(jobRepository, true, step.getName(),
				new CustomPartitioner());
		Set<StepExecution> split = provider.split(stepExecution, 2);
		assertEquals(1, split.size());
		assertEquals("step:foo", split.iterator().next().getStepName());
		stepExecution = update(split, stepExecution, BatchStatus.FAILED);
		split = provider.split(stepExecution, 2);
		assertEquals("step:foo", split.iterator().next().getStepName());
	}

	@Test
	void testGetStepName() {
		SimpleStepExecutionSplitter provider = new SimpleStepExecutionSplitter(jobRepository, true, step.getName(),
				new SimplePartitioner());
		assertEquals("step", provider.getStepName());
	}

	@Test
	void testUnknownStatus() throws Exception {
		SimpleStepExecutionSplitter provider = new SimpleStepExecutionSplitter(jobRepository, true, step.getName(),
				new SimplePartitioner());
		Set<StepExecution> split = provider.split(stepExecution, 2);
		assertEquals(2, split.size());
		stepExecution = update(split, stepExecution, BatchStatus.UNKNOWN);
		try {
			provider.split(stepExecution, 2);
		}
		catch (JobExecutionException e) {
			String message = e.getMessage();
			assertTrue(message.contains("UNKNOWN"), "Wrong message: " + message);
		}
	}

	@Test
	void testCompleteStatusAfterFailure() throws Exception {
		SimpleStepExecutionSplitter provider = new SimpleStepExecutionSplitter(jobRepository, false, step.getName(),
				new SimplePartitioner());
		Set<StepExecution> split = provider.split(stepExecution, 2);
		assertEquals(2, split.size());
		StepExecution nextExecution = update(split, stepExecution, BatchStatus.COMPLETED, false);
		// If already complete in another JobExecution we don't execute again
		assertEquals(0, provider.split(nextExecution, 2).size());
	}

	@Test
	void testCompleteStatusSameJobExecution() throws Exception {
		SimpleStepExecutionSplitter provider = new SimpleStepExecutionSplitter(jobRepository, false, step.getName(),
				new SimplePartitioner());
		Set<StepExecution> split = provider.split(stepExecution, 2);
		assertEquals(2, split.size());
		stepExecution = update(split, stepExecution, BatchStatus.COMPLETED);
		// If already complete in the same JobExecution we should execute again
		assertEquals(2, provider.split(stepExecution, 2).size());
	}

	@Test
	void testIncompleteStatus() throws Exception {
		SimpleStepExecutionSplitter provider = new SimpleStepExecutionSplitter(jobRepository, true, step.getName(),
				new SimplePartitioner());
		Set<StepExecution> split = provider.split(stepExecution, 2);
		assertEquals(2, split.size());
		stepExecution = update(split, stepExecution, BatchStatus.STARTED);
		// If not already complete we don't execute again
		try {
			provider.split(stepExecution, 2);
		}
		catch (JobExecutionException e) {
			String message = e.getMessage();
			assertTrue(message.contains("STARTED"), "Wrong message: " + message);
		}
	}

	@Test
	void testAbandonedStatus() throws Exception {
		SimpleStepExecutionSplitter provider = new SimpleStepExecutionSplitter(jobRepository, true, step.getName(),
				new SimplePartitioner());
		Set<StepExecution> split = provider.split(stepExecution, 2);
		assertEquals(2, split.size());
		stepExecution = update(split, stepExecution, BatchStatus.ABANDONED);
		// If not already complete we don't execute again
		try {
			provider.split(stepExecution, 2);
		}
		catch (JobExecutionException e) {
			String message = e.getMessage();
			assertTrue(message.contains("ABANDONED"), "Wrong message: " + message);
		}
	}

	@Test
	void testResourcelessJobRepositoryThrowsException() throws Exception {
		// Create a ResourcelessJobRepository for testing
		ResourcelessJobRepository resourcelessRepo = new ResourcelessJobRepository();
		JobExecution jobExecution = resourcelessRepo.createJobExecution("job", new JobParameters());
		StepExecution stepExecution = jobExecution.createStepExecution("bar");

		// Create splitter with ResourcelessJobRepository
		SimpleStepExecutionSplitter splitter = new SimpleStepExecutionSplitter(resourcelessRepo, true, step.getName(),
				new SimplePartitioner());

		// Verify that attempting to split with ResourcelessJobRepository throws
		// appropriate exception
		JobExecutionException exception = assertThrows(JobExecutionException.class,
				() -> splitter.split(stepExecution, 2));

		assertTrue(exception.getMessage().contains("ResourcelessJobRepository cannot be used with partitioned steps"));
		assertTrue(exception.getMessage().contains("does not support execution context"));
	}

	private StepExecution update(Set<StepExecution> split, StepExecution stepExecution, BatchStatus status)
			throws Exception {
		return update(split, stepExecution, status, true);
	}

	private StepExecution update(Set<StepExecution> split, StepExecution stepExecution, BatchStatus status,
			boolean sameJobExecution) throws Exception {

		ExecutionContext executionContext = stepExecution.getExecutionContext();

		for (StepExecution child : split) {
			child.setEndTime(LocalDateTime.now());
			child.setStatus(status);
			jobRepository.update(child);
		}

		stepExecution.setEndTime(LocalDateTime.now());
		stepExecution.setStatus(status);
		jobRepository.update(stepExecution);

		JobExecution jobExecution = stepExecution.getJobExecution();
		if (!sameJobExecution) {
			jobExecution.setStatus(BatchStatus.FAILED);
			jobExecution.setEndTime(LocalDateTime.now());
			jobRepository.update(jobExecution);
			JobInstance jobInstance = jobExecution.getJobInstance();
			jobExecution = jobRepository.createJobExecution(jobInstance.getJobName(), jobExecution.getJobParameters());
		}

		stepExecution = jobExecution.createStepExecution(stepExecution.getStepName());
		stepExecution.setExecutionContext(executionContext);

		jobRepository.add(stepExecution);
		return stepExecution;

	}

}
