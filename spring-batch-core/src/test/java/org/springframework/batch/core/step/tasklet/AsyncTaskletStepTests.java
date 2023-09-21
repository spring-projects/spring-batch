/*
 * Copyright 2006-2023 the original author or authors.
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

package org.springframework.batch.core.step.tasklet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.JobRepositorySupport;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.item.support.PassThroughItemProcessor;
import org.springframework.batch.item.support.SynchronizedItemReader;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.repeat.support.TaskExecutorRepeatTemplate;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.util.StringUtils;

class AsyncTaskletStepTests {

	private static final Log logger = LogFactory.getLog(AsyncTaskletStepTests.class);

	private final List<String> processed = new CopyOnWriteArrayList<>();

	private TaskletStep step;

	private int throttleLimit = 20;

	ItemWriter<String> itemWriter = data -> {
		// Thread.sleep(100L);
		logger.info("Items: " + data);
		processed.addAll(data.getItems());
		if (data.getItems().contains("fail")) {
			throw new RuntimeException("Planned");
		}
	};

	private JobRepository jobRepository;

	private List<String> items;

	private int concurrencyLimit = 300;

	private ItemProcessor<String, String> itemProcessor = new PassThroughItemProcessor<>();

	private void setUp() {

		step = new TaskletStep("stepName");

		ResourcelessTransactionManager transactionManager = new ResourcelessTransactionManager();
		step.setTransactionManager(transactionManager);

		RepeatTemplate chunkTemplate = new RepeatTemplate();
		chunkTemplate.setCompletionPolicy(new SimpleCompletionPolicy(2));
		step.setTasklet(new TestingChunkOrientedTasklet<>(new SynchronizedItemReader<>(new ListItemReader<>(items)),
				itemProcessor, itemWriter, chunkTemplate));

		jobRepository = new JobRepositorySupport();
		step.setJobRepository(jobRepository);

		TaskExecutorRepeatTemplate template = new TaskExecutorRepeatTemplate();
		template.setThrottleLimit(throttleLimit);
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		taskExecutor.setConcurrencyLimit(concurrencyLimit);
		template.setTaskExecutor(taskExecutor);
		step.setStepOperations(template);

		step.registerStream(new ItemStream() {
			private int count = 0;

			@Override
			public void update(ExecutionContext executionContext) {
				executionContext.putInt("counter", count++);
			}
		});

	}

	/**
	 * StepExecution should be updated after every chunk commit.
	 */
	@Test
	void testStepExecutionUpdates() throws Exception {

		items = new ArrayList<>(Arrays.asList(StringUtils
			.commaDelimitedListToStringArray("1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25")));

		setUp();

		JobExecution jobExecution = jobRepository.createJobExecution("JOB", new JobParameters());
		StepExecution stepExecution = jobExecution.createStepExecution(step.getName());

		step.execute(stepExecution);

		assertEquals(BatchStatus.COMPLETED, stepExecution.getStatus());
		assertEquals(25, stepExecution.getReadCount());
		assertEquals(25, processed.size());

		// Check commit count didn't spin out of control waiting for other
		// threads to finish...
		assertTrue(stepExecution.getCommitCount() > processed.size() / 2,
				"Not enough commits: " + stepExecution.getCommitCount());
		assertTrue(stepExecution.getCommitCount() <= processed.size() / 2 + throttleLimit + 1,
				"Too many commits: " + stepExecution.getCommitCount());

	}

	/**
	 * StepExecution should fail immediately on error.
	 */
	@Test
	void testStepExecutionFails() throws Exception {

		throttleLimit = 1;
		concurrencyLimit = 1;
		items = Arrays.asList("one", "fail", "three", "four");
		setUp();

		JobExecution jobExecution = jobRepository.createJobExecution("JOB", new JobParameters());
		StepExecution stepExecution = jobExecution.createStepExecution(step.getName());

		step.execute(stepExecution);

		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(2, stepExecution.getReadCount());
		assertEquals(2, processed.size());

	}

	/**
	 * StepExecution should fail immediately on error in processor.
	 */
	@Test
	void testStepExecutionFailsWithProcessor() throws Exception {

		throttleLimit = 1;
		concurrencyLimit = 1;
		items = Arrays.asList("one", "barf", "three", "four");
		itemProcessor = item -> {
			logger.info("Item: " + item);
			processed.add(item);
			if (item.equals("barf")) {
				throw new RuntimeException("Planned processor error");
			}
			return item;
		};
		setUp();

		JobExecution jobExecution = jobRepository.createJobExecution("JOB", new JobParameters());
		StepExecution stepExecution = jobExecution.createStepExecution(step.getName());

		step.execute(stepExecution);

		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(2, stepExecution.getReadCount());
		assertEquals(2, processed.size());

	}

	/**
	 * StepExecution should fail immediately on error.
	 */
	@Test
	void testStepExecutionFailsOnLastItem() throws Exception {

		throttleLimit = 1;
		concurrencyLimit = 1;
		items = Arrays.asList("one", "two", "three", "fail");
		setUp();

		JobExecution jobExecution = jobRepository.createJobExecution("JOB", new JobParameters());
		StepExecution stepExecution = jobExecution.createStepExecution(step.getName());

		step.execute(stepExecution);

		assertEquals(BatchStatus.FAILED, stepExecution.getStatus());
		assertEquals(4, stepExecution.getReadCount());
		assertEquals(4, processed.size());

	}

}
