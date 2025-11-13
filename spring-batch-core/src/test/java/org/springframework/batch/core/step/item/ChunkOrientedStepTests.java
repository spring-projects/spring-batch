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
package org.springframework.batch.core.step.item;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Mahmoud Ben Hassine
 */
public class ChunkOrientedStepTests {

	@Test
	void testFaultTolerantChunkOrientedStepSetupWithDefaultSkipLimit() {
		Assertions.assertDoesNotThrow(() -> new StepBuilder(mock()).chunk(5)
			.reader(new ListItemReader<>(List.of("item1", "item2")))
			.writer(items -> {
			})
			.faultTolerant()
			.skip(Exception.class)
			.build());
	}

	@Test
	void testFaultTolerantChunkOrientedStepSetupWithDefaultRetryLimit() {
		Assertions.assertDoesNotThrow(() -> new StepBuilder(mock()).chunk(5)
			.reader(new ListItemReader<>(List.of("item1", "item2")))
			.writer(items -> {
			})
			.faultTolerant()
			.retry(Exception.class)
			.build());
	}

	@Test
	void testReadNoMoreThanAvailableItemsInSequentialMode() throws Exception {
		// given
		ItemReader<String> reader = mock();
		ItemWriter<String> writer = chunk -> {
		};
		JobRepository jobRepository = new ResourcelessJobRepository();
		when(reader.read()).thenReturn("1", "2", "3", "4", "5", null);
		ChunkOrientedStep<String, String> step = new ChunkOrientedStep<>("step", 10, reader, writer, jobRepository);
		step.afterPropertiesSet();
		JobInstance jobInstance = new JobInstance(1L, "job");
		JobExecution jobExecution = new JobExecution(1L, jobInstance, new JobParameters());
		StepExecution stepExecution = new StepExecution(1L, "step", jobExecution);

		// when
		step.execute(stepExecution);

		// then
		verify(reader, times(6)).read();
	}

	@Test
	void testReadNoMoreThanAvailableItemsInConcurrentMode() throws Exception {
		// given
		ItemReader<String> reader = mock();
		ItemWriter<String> writer = chunk -> {
		};
		JobRepository jobRepository = new ResourcelessJobRepository();
		when(reader.read()).thenReturn("1", "2", "3", "4", "5", null);
		ChunkOrientedStep<String, String> step = new ChunkOrientedStep<>("step", 10, reader, writer, jobRepository);
		step.setTaskExecutor(new SimpleAsyncTaskExecutor());
		step.afterPropertiesSet();
		JobInstance jobInstance = new JobInstance(1L, "job");
		JobExecution jobExecution = new JobExecution(1L, jobInstance, new JobParameters());
		StepExecution stepExecution = new StepExecution(1L, "step", jobExecution);

		// when
		step.execute(stepExecution);

		// then
		verify(reader, times(6)).read();
	}

}
