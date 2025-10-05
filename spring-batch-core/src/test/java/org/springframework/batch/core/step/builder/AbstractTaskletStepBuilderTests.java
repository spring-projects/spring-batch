/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.step.builder;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.repeat.support.TaskExecutorRepeatTemplate;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

/**
 * Test cases for verifying the {@link AbstractTaskletStepBuilder} and faultTolerant()
 * functionality.
 *
 * Issue: https://github.com/spring-projects/spring-batch/issues/4438
 *
 * @author Ilpyo Yang
 * @author Mahmoud Ben Hassine
 */
public class AbstractTaskletStepBuilderTests {

	private final JobRepository jobRepository = mock(JobRepository.class);

	private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);

	private final int chunkSize = 10;

	private final ItemReader<String> itemReader = mock(ItemReader.class);

	private final ItemProcessor<String, String> itemProcessor = mock(ItemProcessor.class);

	private final ItemWriter<String> itemWriter = mock(ItemWriter.class);

	private final SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

	@Test
	void testSetTaskExecutorBeforeFaultTolerant() {
		TaskletStep step = new StepBuilder("step-name", jobRepository)
			.<String, String>chunk(chunkSize, transactionManager)
			.taskExecutor(taskExecutor)
			.reader(itemReader)
			.processor(itemProcessor)
			.writer(itemWriter)
			.faultTolerant()
			.build();

		Object stepOperations = ReflectionTestUtils.getField(step, "stepOperations");
		assertInstanceOf(TaskExecutorRepeatTemplate.class, stepOperations);
	}

	@Test
	void testSetTaskExecutorAfterFaultTolerant() {
		TaskletStep step = new StepBuilder("step-name", jobRepository)
			.<String, String>chunk(chunkSize, transactionManager)
			.reader(itemReader)
			.processor(itemProcessor)
			.writer(itemWriter)
			.faultTolerant()
			.taskExecutor(taskExecutor)
			.build();

		Object stepOperations = ReflectionTestUtils.getField(step, "stepOperations");
		assertInstanceOf(TaskExecutorRepeatTemplate.class, stepOperations);
	}

}
