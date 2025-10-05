/*
 * Copyright 2012-2025 the original author or authors.
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
package org.springframework.batch.core.step.builder;

import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.listener.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.ItemReadListener;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.annotation.AfterChunk;
import org.springframework.batch.core.annotation.AfterChunkError;
import org.springframework.batch.core.annotation.AfterProcess;
import org.springframework.batch.core.annotation.AfterRead;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.AfterWrite;
import org.springframework.batch.core.annotation.BeforeChunk;
import org.springframework.batch.core.annotation.BeforeProcess;
import org.springframework.batch.core.annotation.BeforeRead;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.annotation.BeforeWrite;
import org.springframework.batch.core.configuration.xml.DummyItemReader;
import org.springframework.batch.core.configuration.xml.DummyItemWriter;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemStreamSupport;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.batch.infrastructure.item.support.ListItemWriter;
import org.springframework.batch.infrastructure.item.support.PassThroughItemProcessor;
import org.springframework.batch.infrastructure.repeat.exception.DefaultExceptionHandler;
import org.springframework.batch.infrastructure.repeat.support.RepeatTemplate;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @author Parikshit Dutta
 *
 */
class StepBuilderTests {

	private JobRepository jobRepository;

	private StepExecution execution;

	private PlatformTransactionManager transactionManager;

	@BeforeEach
	void setUp() throws Exception {
		EmbeddedDatabase embeddedDatabase = new EmbeddedDatabaseBuilder()
			.addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
			.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
			.build();
		JdbcTransactionManager transactionManager = new JdbcTransactionManager(embeddedDatabase);
		JdbcJobRepositoryFactoryBean factory = new JdbcJobRepositoryFactoryBean();
		factory.setDataSource(embeddedDatabase);
		factory.setTransactionManager(transactionManager);
		factory.afterPropertiesSet();
		this.jobRepository = factory.getObject();
		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance("foo", jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());
		this.execution = jobRepository.createStepExecution("step", jobExecution);
		this.transactionManager = new ResourcelessTransactionManager();
	}

	@Test
	void test() throws Exception {
		TaskletStepBuilder builder = new StepBuilder("step", jobRepository)
			.tasklet((contribution, chunkContext) -> null, transactionManager);
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
	}

	@Test
	void testListeners() throws Exception {
		TaskletStepBuilder builder = new StepBuilder("step", jobRepository)
			.listener(new InterfaceBasedStepExecutionListener())
			.listener(new AnnotationBasedStepExecutionListener())
			.tasklet((contribution, chunkContext) -> null, transactionManager);
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(1, InterfaceBasedStepExecutionListener.beforeStepCount);
		assertEquals(1, InterfaceBasedStepExecutionListener.afterStepCount);
		assertEquals(1, AnnotationBasedStepExecutionListener.beforeStepCount);
		assertEquals(1, AnnotationBasedStepExecutionListener.afterStepCount);
		assertEquals(1, AnnotationBasedStepExecutionListener.beforeChunkCount);
		assertEquals(1, AnnotationBasedStepExecutionListener.afterChunkCount);
	}

	@Test
	void testAnnotationBasedChunkListenerForTaskletStep() throws Exception {
		TaskletStepBuilder builder = new StepBuilder("step", jobRepository)
			.tasklet((contribution, chunkContext) -> null, transactionManager)
			.listener(new AnnotationBasedChunkListener());
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(1, AnnotationBasedChunkListener.beforeChunkCount);
		assertEquals(1, AnnotationBasedChunkListener.afterChunkCount);
	}

	@Test
	void testAnnotationBasedChunkListenerForSimpleTaskletStep() throws Exception {
		SimpleStepBuilder<Object, Object> builder = new StepBuilder("step", jobRepository).chunk(5, transactionManager)
			.reader(new DummyItemReader())
			.writer(new DummyItemWriter())
			.listener(new AnnotationBasedChunkListener());
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(1, AnnotationBasedChunkListener.beforeChunkCount);
		assertEquals(1, AnnotationBasedChunkListener.afterChunkCount);
	}

	@Test
	void testAnnotationBasedChunkListenerForFaultTolerantTaskletStep() throws Exception {
		SimpleStepBuilder<Object, Object> builder = new StepBuilder("step", jobRepository).chunk(5, transactionManager)
			.reader(new DummyItemReader())
			.writer(new DummyItemWriter())
			.faultTolerant()
			.listener(new AnnotationBasedChunkListener()); // TODO//
															// should
															// this
															// return
															// FaultTolerantStepBuilder?
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(1, AnnotationBasedChunkListener.beforeChunkCount);
		assertEquals(1, AnnotationBasedChunkListener.afterChunkCount);
	}

	@Test
	void testAnnotationBasedChunkListenerForJobStepBuilder() throws Exception {
		SimpleJob job = new SimpleJob("job");
		job.setJobRepository(jobRepository);
		JobStepBuilder builder = new StepBuilder("step", jobRepository).job(job)
			.listener(new AnnotationBasedChunkListener());
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());

		// it makes no sense to register a ChunkListener on a step which is not of type
		// tasklet, so it should not be invoked
		assertEquals(0, AnnotationBasedChunkListener.beforeChunkCount);
		assertEquals(0, AnnotationBasedChunkListener.afterChunkCount);
	}

	@Test
	void testItemListeners() throws Exception {
		List<String> items = Arrays.asList("1", "2", "3");

		ItemReader<String> reader = new ListItemReader<>(items);

		SimpleStepBuilder<String, String> builder = new StepBuilder("step", jobRepository)
			.<String, String>chunk(3, transactionManager)
			.reader(reader)
			.processor(new PassThroughItemProcessor<>())
			.writer(new DummyItemWriter())
			.listener(new AnnotationBasedStepExecutionListener());
		builder.build().execute(execution);

		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(1, AnnotationBasedStepExecutionListener.beforeStepCount);
		assertEquals(1, AnnotationBasedStepExecutionListener.afterStepCount);
		assertEquals(4, AnnotationBasedStepExecutionListener.beforeReadCount);
		assertEquals(3, AnnotationBasedStepExecutionListener.afterReadCount);
		assertEquals(3, AnnotationBasedStepExecutionListener.beforeProcessCount);
		assertEquals(3, AnnotationBasedStepExecutionListener.afterProcessCount);
		assertEquals(1, AnnotationBasedStepExecutionListener.beforeWriteCount);
		assertEquals(1, AnnotationBasedStepExecutionListener.afterWriteCount);
		assertEquals(2, AnnotationBasedStepExecutionListener.beforeChunkCount);
		assertEquals(2, AnnotationBasedStepExecutionListener.afterChunkCount);
	}

	@Test
	void testFunctions() throws Exception {
		assertStepFunctions(false);
	}

	@Test
	void testFunctionsWithFaultTolerantStep() throws Exception {
		assertStepFunctions(true);
	}

	private void assertStepFunctions(boolean faultTolerantStep) throws Exception {
		List<Long> items = Arrays.asList(1L, 2L, 3L);

		ItemReader<Long> reader = new ListItemReader<>(items);

		ListItemWriter<String> itemWriter = new ListItemWriter<>();
		SimpleStepBuilder<Object, String> builder = new StepBuilder("step", jobRepository)
			.<Object, String>chunk(3, transactionManager)
			.reader(reader)
			.processor(Object::toString)
			.writer(itemWriter)
			.listener(new AnnotationBasedStepExecutionListener());

		if (faultTolerantStep) {
			builder = builder.faultTolerant();
		}
		builder.build().execute(execution);

		assertEquals(BatchStatus.COMPLETED, execution.getStatus());

		List<? extends String> writtenItems = itemWriter.getWrittenItems();
		assertEquals("1", writtenItems.get(0));
		assertEquals("2", writtenItems.get(1));
		assertEquals("3", writtenItems.get(2));
	}

	@Test
	void testReturnedTypeOfChunkListenerIsAssignableToSimpleStepBuilder() throws Exception {
		testReturnedTypeOfSetterIsAssignableToSimpleStepBuilder(builder -> builder.listener(new ChunkListener() {
		}));
	}

	@Test
	void testReturnedTypeOfStreamIsAssignableToSimpleStepBuilder() throws Exception {
		testReturnedTypeOfSetterIsAssignableToSimpleStepBuilder(builder -> builder.stream(new ItemStreamSupport() {
		}));
	}

	@Test
	void testReturnedTypeOfTaskExecutorIsAssignableToSimpleStepBuilder() throws Exception {
		testReturnedTypeOfSetterIsAssignableToSimpleStepBuilder(builder -> builder.taskExecutor(null));
	}

	@Test
	void testReturnedTypeOfExceptionHandlerIsAssignableToSimpleStepBuilder() throws Exception {
		testReturnedTypeOfSetterIsAssignableToSimpleStepBuilder(
				builder -> builder.exceptionHandler(new DefaultExceptionHandler()));
	}

	@Test
	void testReturnedTypeOfStepOperationsIsAssignableToSimpleStepBuilder() throws Exception {
		testReturnedTypeOfSetterIsAssignableToSimpleStepBuilder(
				builder -> builder.stepOperations(new RepeatTemplate()));
	}

	@Test
	void testReturnedTypeOfTransactionAttributeIsAssignableToSimpleStepBuilder() throws Exception {
		testReturnedTypeOfSetterIsAssignableToSimpleStepBuilder(builder -> builder.transactionAttribute(null));
	}

	@Test
	void testReturnedTypeOfListenerIsAssignableToSimpleStepBuilder() throws Exception {
		testReturnedTypeOfSetterIsAssignableToSimpleStepBuilder(
				builder -> builder.listener(new AnnotationBasedStepExecutionListener()));
		assertEquals(1, AnnotationBasedStepExecutionListener.beforeStepCount);
		assertEquals(1, AnnotationBasedStepExecutionListener.afterStepCount);
	}

	@Test
	void testReturnedTypeOfExecutionListenerIsAssignableToSimpleStepBuilder() throws Exception {
		testReturnedTypeOfSetterIsAssignableToSimpleStepBuilder(
				builder -> builder.listener(new InterfaceBasedStepExecutionListener()));
		assertEquals(1, InterfaceBasedStepExecutionListener.beforeStepCount);
		assertEquals(1, InterfaceBasedStepExecutionListener.afterStepCount);
	}

	@Test
	void testReturnedTypeOfAllowStartIfCompleteIsAssignableToSimpleStepBuilder() throws Exception {
		testReturnedTypeOfSetterIsAssignableToSimpleStepBuilder(builder -> builder.allowStartIfComplete(false));
	}

	private void testReturnedTypeOfSetterIsAssignableToSimpleStepBuilder(
			UnaryOperator<SimpleStepBuilder<String, String>> configurer) throws Exception {
		List<String> items = Arrays.asList("1", "2", "3");
		ItemReader<String> reader = new ListItemReader<>(items);

		SimpleStepBuilder<String, String> builder = new StepBuilder("step", jobRepository)
			.<String, String>chunk(3, transactionManager)
			.reader(reader)
			.writer(new DummyItemWriter());
		configurer.apply(builder).listener(new InterfaceBasedItemReadListenerListener()).build().execute(execution);

		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(4, InterfaceBasedItemReadListenerListener.beforeReadCount);
		assertEquals(3, InterfaceBasedItemReadListenerListener.afterReadCount);
	}

	public static class InterfaceBasedStepExecutionListener implements StepExecutionListener {

		static int beforeStepCount = 0;
		static int afterStepCount = 0;

		public InterfaceBasedStepExecutionListener() {
			beforeStepCount = 0;
			afterStepCount = 0;
		}

		@Override
		public void beforeStep(StepExecution stepExecution) {
			beforeStepCount++;
		}

		@Override
		public @Nullable ExitStatus afterStep(StepExecution stepExecution) {
			afterStepCount++;
			return stepExecution.getExitStatus();
		}

	}

	public static class InterfaceBasedItemReadListenerListener implements ItemReadListener<String> {

		static int beforeReadCount = 0;
		static int afterReadCount = 0;

		public InterfaceBasedItemReadListenerListener() {
			beforeReadCount = 0;
			afterReadCount = 0;
		}

		@Override
		public void beforeRead() {
			beforeReadCount++;
		}

		@Override
		public void afterRead(String item) {
			afterReadCount++;
		}

		@Override
		public void onReadError(Exception ex) {
		}

	}

	@SuppressWarnings("unused")
	public static class AnnotationBasedStepExecutionListener {

		static int beforeStepCount = 0;
		static int afterStepCount = 0;
		static int beforeReadCount = 0;
		static int afterReadCount = 0;
		static int beforeProcessCount = 0;
		static int afterProcessCount = 0;
		static int beforeWriteCount = 0;
		static int afterWriteCount = 0;
		static int beforeChunkCount = 0;
		static int afterChunkCount = 0;

		public AnnotationBasedStepExecutionListener() {
			beforeStepCount = 0;
			afterStepCount = 0;
			beforeReadCount = 0;
			afterReadCount = 0;
			beforeProcessCount = 0;
			afterProcessCount = 0;
			beforeWriteCount = 0;
			afterWriteCount = 0;
			beforeChunkCount = 0;
			afterChunkCount = 0;
		}

		@BeforeStep
		public void beforeStep() {
			beforeStepCount++;
		}

		@AfterStep
		public ExitStatus afterStep(StepExecution stepExecution) {
			afterStepCount++;
			return stepExecution.getExitStatus();
		}

		@BeforeRead
		public void beforeRead() {
			beforeReadCount++;
		}

		@AfterRead
		public void afterRead() {
			afterReadCount++;
		}

		@BeforeProcess
		public void beforeProcess() {
			beforeProcessCount++;
		}

		@AfterProcess
		public void afterProcess() {
			afterProcessCount++;
		}

		@BeforeWrite
		public void beforeWrite() {
			beforeWriteCount++;
		}

		@AfterWrite
		public void setAfterWrite() {
			afterWriteCount++;
		}

		@BeforeChunk
		public void beforeChunk() {
			beforeChunkCount++;
		}

		@AfterChunk
		public void afterChunk() {
			afterChunkCount++;
		}

	}

	public static class AnnotationBasedChunkListener {

		static int beforeChunkCount = 0;
		static int afterChunkCount = 0;
		static int afterChunkErrorCount = 0;

		public AnnotationBasedChunkListener() {
			beforeChunkCount = 0;
			afterChunkCount = 0;
			afterChunkErrorCount = 0;
		}

		@BeforeChunk
		public void beforeChunk() {
			beforeChunkCount++;
		}

		@AfterChunk
		public void afterChunk() {
			afterChunkCount++;
		}

		@AfterChunkError
		public void afterChunkError() {
			afterChunkErrorCount++;
		}

	}

}
