/*
 * Copyright 2010-2023 the original author or authors.
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
package org.springframework.batch.core.test.step;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.FaultTolerantStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.skip.AlwaysSkipItemSkipPolicy;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.*;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for fault tolerant
 * {@link org.springframework.batch.core.step.item.ChunkOrientedTasklet}.
 */
@SpringJUnitConfig(locations = "/simple-job-launcher-context.xml")
class FaultTolerantStepIntegrationTests {

	private static final int TOTAL_ITEMS = 30;

	private static final int CHUNK_SIZE = TOTAL_ITEMS;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private SkipPolicy skipPolicy;

	private FaultTolerantStepBuilder<Integer, Integer> stepBuilder;

	@BeforeEach
	void setUp() {
		ItemReader<Integer> itemReader = new ListItemReader<>(createItems());
		ItemWriter<Integer> itemWriter = chunk -> {
			if (chunk.getItems().contains(1)) {
				throw new IllegalArgumentException();
			}
		};
		skipPolicy = new SkipIllegalArgumentExceptionSkipPolicy();
		stepBuilder = new StepBuilder("step", jobRepository).<Integer, Integer>chunk(CHUNK_SIZE, transactionManager)
			.reader(itemReader)
			.processor(item -> item > 20 ? null : item)
			.writer(itemWriter)
			.faultTolerant();
	}

	@Test
	void testFilterCountWithTransactionalProcessorWhenSkipInWrite() throws Exception {
		// Given
		Step step = stepBuilder.skipPolicy(skipPolicy).build();

		// When
		StepExecution stepExecution = execute(step);

		// Then
		assertEquals(TOTAL_ITEMS, stepExecution.getReadCount());
		assertEquals(10, stepExecution.getFilterCount());
		assertEquals(19, stepExecution.getWriteCount());
		assertEquals(1, stepExecution.getWriteSkipCount());
	}

	@Test
	void testFilterCountWithNonTransactionalProcessorWhenSkipInWrite() throws Exception {
		// Given
		Step step = stepBuilder.skipPolicy(skipPolicy).processorNonTransactional().build();

		// When
		StepExecution stepExecution = execute(step);

		// Then
		assertEquals(TOTAL_ITEMS, stepExecution.getReadCount());
		assertEquals(10, stepExecution.getFilterCount());
		assertEquals(19, stepExecution.getWriteCount());
		assertEquals(1, stepExecution.getWriteSkipCount());
	}

	@Test
	void testFilterCountOnRetryWithTransactionalProcessorWhenSkipInWrite() throws Exception {
		// Given
		Step step = stepBuilder.retry(IllegalArgumentException.class).retryLimit(2).skipPolicy(skipPolicy).build();

		// When
		StepExecution stepExecution = execute(step);

		// Then
		assertEquals(TOTAL_ITEMS, stepExecution.getReadCount());
		// filter count is expected to be counted on each retry attempt
		assertEquals(20, stepExecution.getFilterCount());
		assertEquals(19, stepExecution.getWriteCount());
		assertEquals(1, stepExecution.getWriteSkipCount());
	}

	@Test
	void testFilterCountOnRetryWithNonTransactionalProcessorWhenSkipInWrite() throws Exception {
		// Given
		Step step = stepBuilder.retry(IllegalArgumentException.class)
			.retryLimit(2)
			.skipPolicy(skipPolicy)
			.processorNonTransactional()
			.build();

		// When
		StepExecution stepExecution = execute(step);

		// Then
		assertEquals(TOTAL_ITEMS, stepExecution.getReadCount());
		// filter count is expected to be counted on each retry attempt
		assertEquals(20, stepExecution.getFilterCount());
		assertEquals(19, stepExecution.getWriteCount());
		assertEquals(1, stepExecution.getWriteSkipCount());
	}

	@Test
	@Timeout(3)
	void testExceptionInProcessDuringChunkScan() throws Exception {
		// Given
		ListItemReader<Integer> itemReader = new ListItemReader<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7));

		ItemProcessor<Integer, Integer> itemProcessor = new ItemProcessor<>() {
			private int cpt;

			@Override
			public @Nullable Integer process(Integer item) throws Exception {
				cpt++;
				if (cpt == 7) { // item 2 succeeds the first time but fails during the
					// scan
					throw new Exception("Error during process");
				}
				return item;
			}
		};

		ItemWriter<Integer> itemWriter = new ItemWriter<>() {
			private int cpt;

			@Override
			public void write(Chunk<? extends Integer> items) throws Exception {
				cpt++;
				if (cpt == 1) {
					throw new Exception("Error during write");
				}
			}
		};

		Step step = new StepBuilder("step", jobRepository).<Integer, Integer>chunk(5, transactionManager)
			.reader(itemReader)
			.processor(itemProcessor)
			.writer(itemWriter)
			.faultTolerant()
			.skip(Exception.class)
			.skipLimit(3)
			.build();

		// When
		StepExecution stepExecution = execute(step);

		// Then
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(ExitStatus.COMPLETED, stepExecution.getExitStatus());
		assertEquals(7, stepExecution.getReadCount());
		assertEquals(6, stepExecution.getWriteCount());
		assertEquals(1, stepExecution.getProcessSkipCount());
	}

	@Test
	@Timeout(3000)
	void testExceptionInProcessAndWriteDuringChunkScan() throws Exception {
		// Given
		ListItemReader<Integer> itemReader = new ListItemReader<>(Arrays.asList(1, 2, 3));

		ItemProcessor<Integer, Integer> itemProcessor = item -> {
			if (item.equals(2)) {
				throw new Exception("Error during process item " + item);
			}
			return item;
		};

		ItemWriter<Integer> itemWriter = chunk -> {
			if (chunk.getItems().contains(3)) {
				throw new Exception("Error during write");
			}
		};

		Step step = new StepBuilder("step", jobRepository).<Integer, Integer>chunk(5, transactionManager)
			.reader(itemReader)
			.processor(itemProcessor)
			.writer(itemWriter)
			.faultTolerant()
			.skipPolicy(new AlwaysSkipItemSkipPolicy())
			.build();

		// When
		StepExecution stepExecution = execute(step);

		// Then
		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(ExitStatus.COMPLETED, stepExecution.getExitStatus());
		assertEquals(3, stepExecution.getReadCount());
		assertEquals(1, stepExecution.getWriteCount());
		assertEquals(1, stepExecution.getWriteSkipCount());
		assertEquals(1, stepExecution.getProcessSkipCount());
		assertEquals(3, stepExecution.getRollbackCount());
		assertEquals(2, stepExecution.getCommitCount());
	}

	private List<Integer> createItems() {
		List<Integer> items = new ArrayList<>(TOTAL_ITEMS);
		for (int i = 1; i <= TOTAL_ITEMS; i++) {
			items.add(i);
		}
		return items;
	}

	private StepExecution execute(Step step) throws Exception {
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance("job" + Math.random(), jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());
		StepExecution stepExecution = jobRepository.createStepExecution("step", jobExecution);
		step.execute(stepExecution);
		return stepExecution;
	}

	private static class SkipIllegalArgumentExceptionSkipPolicy implements SkipPolicy {

		@Override
		public boolean shouldSkip(Throwable throwable, long skipCount) throws SkipLimitExceededException {
			return throwable instanceof IllegalArgumentException;
		}

	}

}
