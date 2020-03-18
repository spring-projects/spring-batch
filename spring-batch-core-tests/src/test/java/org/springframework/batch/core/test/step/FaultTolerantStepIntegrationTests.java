/*
 * Copyright 2010-2019 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.FaultTolerantStepBuilder;
import org.springframework.batch.core.step.skip.AlwaysSkipItemSkipPolicy;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.Assert.assertEquals;

/**
 * Tests for fault tolerant {@link org.springframework.batch.core.step.item.ChunkOrientedTasklet}.
 */
@ContextConfiguration(locations = "/simple-job-launcher-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class FaultTolerantStepIntegrationTests {
	
	private static final int TOTAL_ITEMS = 30;
	private static final int CHUNK_SIZE = TOTAL_ITEMS;
	
	@Autowired
	private JobRepository jobRepository;
	
	@Autowired
	private PlatformTransactionManager transactionManager;
	
	private SkipPolicy skipPolicy;
	
	private FaultTolerantStepBuilder<Integer, Integer> stepBuilder;
	
	@Before
	public void setUp() {
		ItemReader<Integer> itemReader = new ListItemReader<>(createItems());
		ItemProcessor<Integer, Integer> itemProcessor = item -> item > 20 ? null : item;
		ItemWriter<Integer> itemWriter = chunk -> {
			if (chunk.contains(1)) {
				throw new IllegalArgumentException();
			}
		};
		skipPolicy = new SkipIllegalArgumentExceptionSkipPolicy();
		stepBuilder = new StepBuilderFactory(jobRepository, transactionManager).get("step")
				.<Integer, Integer>chunk(CHUNK_SIZE)
				.reader(itemReader)
				.processor(itemProcessor)
				.writer(itemWriter)
				.faultTolerant();
	}
	
	@Test
	public void testFilterCountWithTransactionalProcessorWhenSkipInWrite() throws Exception {
		// Given
		Step step = stepBuilder
				.skipPolicy(skipPolicy)
				.build();
		
		// When
		StepExecution stepExecution = execute(step);
		
		// Then
		assertEquals(TOTAL_ITEMS, stepExecution.getReadCount());
		assertEquals(10, stepExecution.getFilterCount());
		assertEquals(19, stepExecution.getWriteCount());
		assertEquals(1, stepExecution.getWriteSkipCount());
	}
	
	@Test
	public void testFilterCountWithNonTransactionalProcessorWhenSkipInWrite() throws Exception {
		// Given
		Step step = stepBuilder
				.skipPolicy(skipPolicy)
				.processorNonTransactional()
				.build();
		
		// When
		StepExecution stepExecution = execute(step);
		
		// Then
		assertEquals(TOTAL_ITEMS, stepExecution.getReadCount());
		assertEquals(10, stepExecution.getFilterCount());
		assertEquals(19, stepExecution.getWriteCount());
		assertEquals(1, stepExecution.getWriteSkipCount());
	}
	
	@Test
	public void testFilterCountOnRetryWithTransactionalProcessorWhenSkipInWrite() throws Exception {
		// Given
		Step step = stepBuilder
				.retry(IllegalArgumentException.class)
				.retryLimit(2)
				.skipPolicy(skipPolicy)
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
	public void testFilterCountOnRetryWithNonTransactionalProcessorWhenSkipInWrite() throws Exception {
		// Given
		Step step = stepBuilder
				.retry(IllegalArgumentException.class)
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

	@Test(timeout = 3000)
	public void testExceptionInProcessDuringChunkScan() throws Exception {
		// Given
		ListItemReader<Integer> itemReader = new ListItemReader<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7));

		ItemProcessor<Integer, Integer> itemProcessor = new ItemProcessor<Integer, Integer>() {
			private int cpt;

			@Nullable
			@Override
			public Integer process(Integer item) throws Exception {
				cpt++;
				if (cpt == 7) { // item 2 succeeds the first time but fails during the scan
					throw new Exception("Error during process");
				}
				return item;
			}
		};

		ItemWriter<Integer> itemWriter = new ItemWriter<Integer>() {
			private int cpt;

			@Override
			public void write(List<? extends Integer> items) throws Exception {
				cpt++;
				if (cpt == 1) {
					throw new Exception("Error during write");
				}
			}
		};

		Step step = new StepBuilderFactory(jobRepository, transactionManager).get("step")
				.<Integer, Integer>chunk(5)
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

	@Test(timeout = 3000)
	public void testExceptionInProcessAndWriteDuringChunkScan() throws Exception {
		// Given
		ListItemReader<Integer> itemReader = new ListItemReader<>(Arrays.asList(1, 2, 3));

		ItemProcessor<Integer, Integer> itemProcessor = new ItemProcessor<Integer, Integer>() {
			@Override
			public Integer process(Integer item) throws Exception {
				if (item.equals(2)) {
					throw new Exception("Error during process item " + item);
				}
				return item;
			}
		};

		ItemWriter<Integer> itemWriter = new ItemWriter<Integer>() {
			@Override
			public void write(List<? extends Integer> items) throws Exception {
				if (items.contains(3)) {
					throw new Exception("Error during write");
				}
			}
		};

		Step step = new StepBuilderFactory(jobRepository, transactionManager).get("step")
				.<Integer, Integer>chunk(5)
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
		JobExecution jobExecution = jobRepository.createJobExecution(
								"job" + Math.random(), new JobParameters());
		StepExecution stepExecution = jobExecution.createStepExecution("step");
		jobRepository.add(stepExecution);
		step.execute(stepExecution);
		return stepExecution;
	}
	
	private class SkipIllegalArgumentExceptionSkipPolicy implements SkipPolicy {
		
		@Override
		public boolean shouldSkip(Throwable throwable, int skipCount)
				throws SkipLimitExceededException {
			return throwable instanceof IllegalArgumentException;
		}
		
	}
}
