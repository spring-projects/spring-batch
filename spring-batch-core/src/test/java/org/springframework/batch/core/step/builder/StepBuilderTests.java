/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.junit.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobParameters;
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
import org.springframework.batch.core.configuration.xml.DummyItemReader;
import org.springframework.batch.core.configuration.xml.DummyItemWriter;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.item.support.ListItemWriter;
import org.springframework.batch.item.support.PassThroughItemProcessor;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.Assert.assertEquals;

/**
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 *
 */
@SuppressWarnings("serial")
public class StepBuilderTests {

	@Test
	public void test() throws Exception {
		JobRepository jobRepository = new MapJobRepositoryFactoryBean().getObject();
		StepExecution execution = jobRepository.createJobExecution("foo", new JobParameters()).createStepExecution(
				"step");
		jobRepository.add(execution);
		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();
		TaskletStepBuilder builder = new StepBuilder("step").repository(jobRepository)
				.transactionManager(transactionManager).tasklet((contribution, chunkContext) -> null);
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
	}

	@Test
	public void testListeners() throws Exception {
		JobRepository jobRepository = new MapJobRepositoryFactoryBean().getObject();
		StepExecution execution = jobRepository.createJobExecution("foo", new JobParameters()).createStepExecution("step");
		jobRepository.add(execution);
		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();
		TaskletStepBuilder builder = new StepBuilder("step")
											 .repository(jobRepository)
											 .transactionManager(transactionManager)
											 .listener(new InterfaceBasedStepExecutionListener())
											 .listener(new AnnotationBasedStepExecutionListener())
											 .tasklet((contribution, chunkContext) -> null);
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
	public void testAnnotationBasedChunkListenerForTaskletStep() throws Exception {
		JobRepository jobRepository = new MapJobRepositoryFactoryBean().getObject();
		StepExecution execution = jobRepository.createJobExecution("foo", new JobParameters()).createStepExecution("step");
		jobRepository.add(execution);
		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();
		TaskletStepBuilder builder = new StepBuilder("step")
				.repository(jobRepository)
				.transactionManager(transactionManager)
				.tasklet((contribution, chunkContext) -> null)
				.listener(new AnnotationBasedChunkListener());
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(1, AnnotationBasedChunkListener.beforeChunkCount);
		assertEquals(1, AnnotationBasedChunkListener.afterChunkCount);
	}

	@Test
	public void testAnnotationBasedChunkListenerForSimpleTaskletStep() throws Exception {
		JobRepository jobRepository = new MapJobRepositoryFactoryBean().getObject();
		StepExecution execution = jobRepository.createJobExecution("foo", new JobParameters()).createStepExecution("step");
		jobRepository.add(execution);
		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();
		SimpleStepBuilder<Object, Object> builder = new StepBuilder("step")
				.repository(jobRepository)
				.transactionManager(transactionManager)
				.chunk(5)
				.reader(new DummyItemReader())
				.writer(new DummyItemWriter())
				.listener(new AnnotationBasedChunkListener());
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(1, AnnotationBasedChunkListener.beforeChunkCount);
		assertEquals(1, AnnotationBasedChunkListener.afterChunkCount);
	}

	@Test
	public void testAnnotationBasedChunkListenerForFaultTolerantTaskletStep() throws Exception {
		JobRepository jobRepository = new MapJobRepositoryFactoryBean().getObject();
		StepExecution execution = jobRepository.createJobExecution("foo", new JobParameters()).createStepExecution("step");
		jobRepository.add(execution);
		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();
		SimpleStepBuilder<Object, Object> builder = new StepBuilder("step")
				.repository(jobRepository)
				.transactionManager(transactionManager)
				.chunk(5)
				.reader(new DummyItemReader())
				.writer(new DummyItemWriter())
				.faultTolerant()
				.listener(new AnnotationBasedChunkListener()); // TODO should this return FaultTolerantStepBuilder?
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(1, AnnotationBasedChunkListener.beforeChunkCount);
		assertEquals(1, AnnotationBasedChunkListener.afterChunkCount);
	}

	@Test
	public void testAnnotationBasedChunkListenerForJobStepBuilder() throws Exception {
		JobRepository jobRepository = new MapJobRepositoryFactoryBean().getObject();
		StepExecution execution = jobRepository.createJobExecution("foo", new JobParameters()).createStepExecution("step");
		jobRepository.add(execution);
		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();
		SimpleJob job = new SimpleJob("job");
		job.setJobRepository(jobRepository);
		JobStepBuilder builder = new StepBuilder("step")
				.repository(jobRepository)
				.transactionManager(transactionManager)
				.job(job)
				.listener(new AnnotationBasedChunkListener());
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());

		// it makes no sense to register a ChunkListener on a step which is not of type tasklet, so it should not be invoked
		assertEquals(0, AnnotationBasedChunkListener.beforeChunkCount);
		assertEquals(0, AnnotationBasedChunkListener.afterChunkCount);
	}

	@Test
	public void testItemListeners() throws Exception {
		JobRepository jobRepository = new MapJobRepositoryFactoryBean().getObject();
		StepExecution execution = jobRepository.createJobExecution("foo", new JobParameters()).createStepExecution("step");
		jobRepository.add(execution);
		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

		List<String> items = new ArrayList<String>() {{
			add("1");
			add("2");
			add("3");
		}};

		ItemReader<String> reader = new ListItemReader<>(items);

		@SuppressWarnings("unchecked")
		SimpleStepBuilder<String, String> builder = new StepBuilder("step")
											 .repository(jobRepository)
											 .transactionManager(transactionManager)
											 .<String, String>chunk(3)
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
	public void testFunctions() throws Exception {
		JobRepository jobRepository = new MapJobRepositoryFactoryBean().getObject();
		StepExecution execution = jobRepository.createJobExecution("foo", new JobParameters()).createStepExecution("step");
		jobRepository.add(execution);
		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

		List<Long> items = new ArrayList<Long>() {{
			add(1L);
			add(2L);
			add(3L);
		}};

		ItemReader<Long> reader = new ListItemReader<>(items);

		ListItemWriter<String> itemWriter = new ListItemWriter<>();
		@SuppressWarnings("unchecked")
		SimpleStepBuilder<Object, String> builder = new StepBuilder("step")
											 .repository(jobRepository)
											 .transactionManager(transactionManager)
											 .<Object, String>chunk(3)
											 .reader(reader)
											 .processor((Function<Object, String>) s -> s.toString())
											 .writer(itemWriter)
											 .listener(new AnnotationBasedStepExecutionListener());
		builder.build().execute(execution);

		assertEquals(BatchStatus.COMPLETED, execution.getStatus());

		List<? extends String> writtenItems = itemWriter.getWrittenItems();
		assertEquals("1", writtenItems.get(0));
		assertEquals("2", writtenItems.get(1));
		assertEquals("3", writtenItems.get(2));
	}

	public static class InterfaceBasedStepExecutionListener implements StepExecutionListener {

		static int beforeStepCount = 0;
		static int afterStepCount = 0;

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
