package org.springframework.batch.test;
/*
 * Copyright 2020-2022 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.AbstractTaskletStepBuilder;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilderHelper;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Test cases for verifying the {@link AbstractTaskletStepBuilder} and faultTolerant() functionality.
 *
 * @author Ilpyo Yang
 */
@SpringBatchTest
@SpringJUnitConfig
public class AbstractTaskletStepBuilderTests {
	private final JobRepository jobRepository =  mock(JobRepository.class);
	private final int chunkSize = 10;
	private final ItemReader itemReader = mock(ItemReader.class);
	private final ItemProcessor itemProcessor = mock(ItemProcessor.class);
	private final ItemWriter itemWriter = mock(ItemWriter.class);
	private final SimpleAsyncTaskExecutor taskExecutor  = new SimpleAsyncTaskExecutor();
	SimpleStepBuilder simpleStepBuilder;

	private <T> T accessPrivateField(Object o, String fieldName) throws ReflectiveOperationException {
		Field field = o.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return (T) field.get(o);
	}

	private <T> T accessSuperClassPrivateField(Object o, String fieldName) throws ReflectiveOperationException {
		Field field = o.getClass().getSuperclass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return (T) field.get(o);
	}

	@BeforeEach
	void set(){
		StepBuilderHelper stepBuilderHelper = new StepBuilderHelper("test", jobRepository) {
			@Override
			protected StepBuilderHelper self() {
				return null;
			}
		};
		simpleStepBuilder = new SimpleStepBuilder(stepBuilderHelper);
		simpleStepBuilder.chunk(chunkSize);
		simpleStepBuilder.reader(itemReader);
		simpleStepBuilder.processor(itemProcessor);
		simpleStepBuilder.writer(itemWriter);
	}

	@Test
	void copyConstractorTest() throws ReflectiveOperationException {
		Constructor<SimpleStepBuilder> constructor = SimpleStepBuilder.class.getDeclaredConstructor(SimpleStepBuilder.class);
		constructor.setAccessible(true);
		SimpleStepBuilder copySimpleStepBuilder = constructor.newInstance(simpleStepBuilder);

		int copyChunkSize = accessPrivateField(copySimpleStepBuilder, "chunkSize");
		ItemReader copyItemReader = accessPrivateField(copySimpleStepBuilder, "reader");
		ItemProcessor copyItemProcessor = accessPrivateField(copySimpleStepBuilder, "processor");
		ItemWriter copyItemWriter = accessPrivateField(copySimpleStepBuilder, "writer");

		assertEquals(chunkSize, copyChunkSize);
		assertEquals(itemReader, copyItemReader);
		assertEquals(itemProcessor, copyItemProcessor);
		assertEquals(itemWriter, copyItemWriter);
	}

	@Test
	void faultTolerantMethodTest() throws ReflectiveOperationException {
		simpleStepBuilder.taskExecutor(taskExecutor); // The task executor is set before faultTolerant()
		simpleStepBuilder.faultTolerant();

		int afterChunkSize = accessPrivateField(simpleStepBuilder, "chunkSize");
		ItemReader afterItemReader = accessPrivateField(simpleStepBuilder, "reader");
		ItemProcessor afterItemProcessor = accessPrivateField(simpleStepBuilder, "processor");
		ItemWriter afterItemWriter = accessPrivateField(simpleStepBuilder, "writer");
		TaskExecutor afterTaskExecutor = accessSuperClassPrivateField(simpleStepBuilder, "taskExecutor");

		assertEquals(chunkSize, afterChunkSize);
		assertEquals(itemReader, afterItemReader);
		assertEquals(itemProcessor, afterItemProcessor);
		assertEquals(itemWriter, afterItemWriter);
		assertEquals(taskExecutor, afterTaskExecutor);
	}
}
