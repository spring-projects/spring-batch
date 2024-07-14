/*
 * Copyright 2012-2024 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
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
import org.springframework.batch.core.annotation.OnProcessError;
import org.springframework.batch.core.annotation.OnReadError;
import org.springframework.batch.core.annotation.OnSkipInProcess;
import org.springframework.batch.core.annotation.OnSkipInRead;
import org.springframework.batch.core.annotation.OnSkipInWrite;
import org.springframework.batch.core.annotation.OnWriteError;
import org.springframework.batch.core.configuration.xml.DummyItemReader;
import org.springframework.batch.core.configuration.xml.DummyItemWriter;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.item.support.ListItemWriter;
import org.springframework.batch.item.support.PassThroughItemProcessor;
import org.springframework.batch.repeat.exception.DefaultExceptionHandler;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @author Parikshit Dutta
 * @author Seonkyo Ok
 *
 */
class StepBuilderTests {

	private JobRepository jobRepository;

	private StepExecution execution;

	private PlatformTransactionManager transactionManager;

	@BeforeEach
	void setUp() throws Exception {
		final EmbeddedDatabase embeddedDatabase = new EmbeddedDatabaseBuilder()
			.addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
			.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
			.build();
		final JdbcTransactionManager transactionManager = new JdbcTransactionManager(embeddedDatabase);
		final JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
		factory.setDataSource(embeddedDatabase);
		factory.setTransactionManager(transactionManager);
		factory.afterPropertiesSet();
		this.jobRepository = factory.getObject();
		this.execution = this.jobRepository.createJobExecution("foo", new JobParameters()).createStepExecution("step");
		this.jobRepository.add(this.execution);
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
	void testInterfaceBasedStepListener() throws Exception {
		final List<Integer> items = Arrays.asList(1, 2, 3, 4);
		final ItemReader<Integer> reader = new ListItemReader<>(items);

		final InterfaceBasedStepListener stepInterfaceListener = new InterfaceBasedStepListener();
		final InterfaceAndAnnotationBasedStepListener stepInterfaceAndAnnotationListener = new InterfaceAndAnnotationBasedStepListener();
		final TaskletStep step = new StepBuilder("step", jobRepository).<Integer, Integer>chunk(2, transactionManager)
			.reader(reader)
			.processor(new PassThroughItemProcessor<>())
			.writer(new ListItemWriter<>())
			.listener((Object) stepInterfaceListener)
			.listener((Object) stepInterfaceAndAnnotationListener)
			.build();
		step.execute(execution);

		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(1, stepInterfaceListener.beforeStepCount);
		assertEquals(1, stepInterfaceAndAnnotationListener.beforeStepCount);
		assertEquals(1, stepInterfaceListener.afterStepCount);
		assertEquals(1, stepInterfaceAndAnnotationListener.afterStepCount);
		assertEquals(3, stepInterfaceListener.beforeChunkCount);
		assertEquals(3, stepInterfaceAndAnnotationListener.beforeChunkCount);
		assertEquals(3, stepInterfaceListener.afterChunkCount);
		assertEquals(3, stepInterfaceAndAnnotationListener.afterChunkCount);
		assertEquals(5, stepInterfaceListener.beforeReadCount);
		assertEquals(5, stepInterfaceAndAnnotationListener.beforeReadCount);
		assertEquals(4, stepInterfaceListener.afterReadCount);
		assertEquals(4, stepInterfaceAndAnnotationListener.afterReadCount);
		assertEquals(0, stepInterfaceListener.onReadErrorCount);
		assertEquals(0, stepInterfaceAndAnnotationListener.onReadErrorCount);
		assertEquals(4, stepInterfaceListener.beforeProcessCount);
		assertEquals(4, stepInterfaceAndAnnotationListener.beforeProcessCount);
		assertEquals(4, stepInterfaceListener.afterProcessCount);
		assertEquals(4, stepInterfaceAndAnnotationListener.afterProcessCount);
		assertEquals(0, stepInterfaceListener.onProcessErrorCount);
		assertEquals(0, stepInterfaceAndAnnotationListener.onProcessErrorCount);
		assertEquals(2, stepInterfaceListener.beforeWriteCount);
		assertEquals(2, stepInterfaceAndAnnotationListener.beforeWriteCount);
		assertEquals(2, stepInterfaceListener.afterWriteCount);
		assertEquals(2, stepInterfaceAndAnnotationListener.afterWriteCount);
		assertEquals(0, stepInterfaceListener.onWriteErrorCount);
		assertEquals(0, stepInterfaceAndAnnotationListener.onWriteErrorCount);
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
	void testReturnedTypeOfThrottleLimitIsAssignableToSimpleStepBuilder() throws Exception {
		testReturnedTypeOfSetterIsAssignableToSimpleStepBuilder(builder -> builder.throttleLimit(4));
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
		configurer.apply(builder).listener(new InterfaceBasedItemReadListener()).build().execute(execution);

		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(4, InterfaceBasedItemReadListener.beforeReadCount);
		assertEquals(3, InterfaceBasedItemReadListener.afterReadCount);
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

		@Nullable
		@Override
		public ExitStatus afterStep(StepExecution stepExecution) {
			afterStepCount++;
			return stepExecution.getExitStatus();
		}

	}

	public static class InterfaceBasedItemReadListener implements ItemReadListener<String> {

		static int beforeReadCount = 0;
		static int afterReadCount = 0;

		public InterfaceBasedItemReadListener() {
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

	private static class InterfaceBasedStepListener
			implements StepExecutionListener, ItemReadListener<Integer>, ItemProcessListener<Integer, Integer>,
			ItemWriteListener<Integer>, ChunkListener, SkipListener<Integer, Integer> {

		int beforeStepCount;

		int afterStepCount;

		int beforeReadCount;

		int afterReadCount;

		int onReadErrorCount;

		int beforeProcessCount;

		int afterProcessCount;

		int onProcessErrorCount;

		int beforeWriteCount;

		int afterWriteCount;

		int onWriteErrorCount;

		int beforeChunkCount;

		int afterChunkCount;

		int afterChunkErrorCount;

		int onSkipInReadCount;

		int onSkipInProcessCount;

		int onSkipInWriteCount;

		@Override
		public void beforeStep(StepExecution stepExecution) {
			beforeStepCount++;
		}

		@Override
		@Nullable
		public ExitStatus afterStep(StepExecution stepExecution) {
			afterStepCount++;
			return null;
		}

		@Override
		public void beforeRead() {
			beforeReadCount++;
		}

		@Override
		public void afterRead(Integer item) {
			afterReadCount++;
		}

		@Override
		public void onReadError(Exception ex) {
			onReadErrorCount++;
		}

		@Override
		public void beforeProcess(Integer item) {
			beforeProcessCount++;
		}

		@Override
		public void afterProcess(Integer item, @Nullable Integer result) {
			afterProcessCount++;
		}

		@Override
		public void onProcessError(Integer item, Exception e) {
			onProcessErrorCount++;
		}

		@Override
		public void beforeWrite(Chunk<? extends Integer> items) {
			beforeWriteCount++;
		}

		@Override
		public void afterWrite(Chunk<? extends Integer> items) {
			afterWriteCount++;
		}

		@Override
		public void onWriteError(Exception exception, Chunk<? extends Integer> items) {
			onWriteErrorCount++;
		}

		@Override
		public void beforeChunk(ChunkContext context) {
			beforeChunkCount++;
		}

		@Override
		public void afterChunk(ChunkContext context) {
			afterChunkCount++;
		}

		@Override
		public void afterChunkError(ChunkContext context) {
			afterChunkErrorCount++;
		}

		@Override
		public void onSkipInProcess(Integer item, Throwable t) {
			onSkipInProcessCount++;
		}

		@Override
		public void onSkipInRead(Throwable t) {
			onSkipInReadCount++;
		}

		@Override
		public void onSkipInWrite(Integer item, Throwable t) {
			onSkipInWriteCount++;
		}

	}

	private static class InterfaceAndAnnotationBasedStepListener
			implements StepExecutionListener, ItemReadListener<Integer>, ItemProcessListener<Integer, Integer>,
			ItemWriteListener<Integer>, ChunkListener, SkipListener<Integer, Integer> {

		int beforeStepCount;

		int afterStepCount;

		int beforeReadCount;

		int afterReadCount;

		int onReadErrorCount;

		int beforeProcessCount;

		int afterProcessCount;

		int onProcessErrorCount;

		int beforeWriteCount;

		int afterWriteCount;

		int onWriteErrorCount;

		int beforeChunkCount;

		int afterChunkCount;

		int afterChunkErrorCount;

		int onSkipInReadCount;

		int onSkipInProcessCount;

		int onSkipInWriteCount;

		@Override
		@BeforeStep
		public void beforeStep(StepExecution stepExecution) {
			beforeStepCount++;
		}

		@Override
		@Nullable
		@AfterStep
		public ExitStatus afterStep(StepExecution stepExecution) {
			afterStepCount++;
			return null;
		}

		@Override
		@BeforeRead
		public void beforeRead() {
			beforeReadCount++;
		}

		@Override
		@AfterRead
		public void afterRead(Integer item) {
			afterReadCount++;
		}

		@Override
		@OnReadError
		public void onReadError(Exception ex) {
			onReadErrorCount++;
		}

		@Override
		@BeforeProcess
		public void beforeProcess(Integer item) {
			beforeProcessCount++;
		}

		@Override
		@AfterProcess
		public void afterProcess(Integer item, @Nullable Integer result) {
			afterProcessCount++;
		}

		@Override
		@OnProcessError
		public void onProcessError(Integer item, Exception e) {
			onProcessErrorCount++;
		}

		@Override
		@BeforeWrite
		public void beforeWrite(Chunk<? extends Integer> items) {
			beforeWriteCount++;
		}

		@Override
		@AfterWrite
		public void afterWrite(Chunk<? extends Integer> items) {
			afterWriteCount++;
		}

		@Override
		@OnWriteError
		public void onWriteError(Exception exception, Chunk<? extends Integer> items) {
			onWriteErrorCount++;
		}

		@Override
		@BeforeChunk
		public void beforeChunk(ChunkContext context) {
			beforeChunkCount++;
		}

		@Override
		@AfterChunk
		public void afterChunk(ChunkContext context) {
			afterChunkCount++;
		}

		@Override
		@AfterChunkError
		public void afterChunkError(ChunkContext context) {
			afterChunkErrorCount++;
		}

		@Override
		@OnSkipInProcess
		public void onSkipInProcess(Integer item, Throwable t) {
			onSkipInProcessCount++;
		}

		@Override
		@OnSkipInRead
		public void onSkipInRead(Throwable t) {
			onSkipInReadCount++;
		}

		@Override
		@OnSkipInWrite
		public void onSkipInWrite(Integer item, Throwable t) {
			onSkipInWriteCount++;
		}

	}

}
