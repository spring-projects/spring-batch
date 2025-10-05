/*
 * Copyright 2018-2025 the original author or authors.
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
package org.springframework.batch.integration.chunk;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.listener.ChunkListener;
import org.springframework.batch.core.listener.ItemReadListener;
import org.springframework.batch.core.listener.ItemWriteListener;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.item.ChunkOrientedTasklet;
import org.springframework.batch.core.step.item.SimpleChunkProcessor;
import org.springframework.batch.core.step.item.SimpleChunkProvider;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemStreamSupport;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.CompositeItemStream;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.batch.infrastructure.repeat.support.RepeatTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.messaging.PollableChannel;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.MapRetryContextCache;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Mahmoud Ben Hassine
 */
@SpringJUnitConfig(classes = { RemoteChunkingManagerStepBuilderTests.BatchConfiguration.class })
class RemoteChunkingManagerStepBuilderTests {

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private final PollableChannel inputChannel = new QueueChannel();

	private final DirectChannel outputChannel = new DirectChannel();

	private final ItemReader<String> itemReader = new ListItemReader<>(Arrays.asList("a", "b", "c"));

	@Test
	void inputChannelMustNotBeNull() {
		// when
		final Exception expectedException = assertThrows(IllegalArgumentException.class,
				() -> new RemoteChunkingManagerStepBuilder<String, String>("step", this.jobRepository)
					.inputChannel(null)
					.build());

		// then
		assertThat(expectedException).hasMessage("inputChannel must not be null");
	}

	@Test
	void outputChannelMustNotBeNull() {
		// when
		final Exception expectedException = assertThrows(IllegalArgumentException.class,
				() -> new RemoteChunkingManagerStepBuilder<String, String>("step", this.jobRepository)
					.outputChannel(null)
					.build());

		// then
		assertThat(expectedException).hasMessage("outputChannel must not be null");
	}

	@Test
	void messagingTemplateMustNotBeNull() {
		// when
		final Exception expectedException = assertThrows(IllegalArgumentException.class,
				() -> new RemoteChunkingManagerStepBuilder<String, String>("step", this.jobRepository)
					.messagingTemplate(null)
					.build());

		// then
		assertThat(expectedException).hasMessage("messagingTemplate must not be null");
	}

	@Test
	void maxWaitTimeoutsMustBeGreaterThanZero() {
		// when
		final Exception expectedException = assertThrows(IllegalArgumentException.class,
				() -> new RemoteChunkingManagerStepBuilder<String, String>("step", this.jobRepository)
					.maxWaitTimeouts(-1)
					.build());

		// then
		assertThat(expectedException).hasMessage("maxWaitTimeouts must be greater than zero");
	}

	@Test
	void throttleLimitMustNotBeGreaterThanZero() {
		// when
		final Exception expectedException = assertThrows(IllegalArgumentException.class,
				() -> new RemoteChunkingManagerStepBuilder<String, String>("step", this.jobRepository)
					.throttleLimit(-1L)
					.build());

		// then
		assertThat(expectedException).hasMessage("throttleLimit must be greater than zero");
	}

	@Test
	void testMandatoryInputChannel() {
		// given
		RemoteChunkingManagerStepBuilder<String, String> builder = new RemoteChunkingManagerStepBuilder<>("step",
				this.jobRepository);

		// when
		final Exception expectedException = assertThrows(IllegalArgumentException.class, builder::build);

		// then
		assertThat(expectedException).hasMessage("An InputChannel must be provided");
	}

	@Test
	void eitherOutputChannelOrMessagingTemplateMustBeProvided() {
		// given
		RemoteChunkingManagerStepBuilder<String, String> builder = new RemoteChunkingManagerStepBuilder<String, String>(
				"step", this.jobRepository)
			.inputChannel(this.inputChannel)
			.outputChannel(new DirectChannel())
			.messagingTemplate(new MessagingTemplate());

		// when
		final Exception expectedException = assertThrows(IllegalStateException.class, builder::build);

		// then
		assertThat(expectedException)
			.hasMessage("You must specify either an outputChannel or a messagingTemplate but not both.");
	}

	@Test
	void testUnsupportedOperationExceptionWhenSpecifyingAnItemWriter() {
		// when
		final Exception expectedException = assertThrows(UnsupportedOperationException.class,
				() -> new RemoteChunkingManagerStepBuilder<String, String>("step", this.jobRepository)
					.reader(this.itemReader)
					.writer(items -> {
					})
					.transactionManager(this.transactionManager)
					.inputChannel(this.inputChannel)
					.outputChannel(this.outputChannel)
					.build());

		// then
		assertThat(expectedException).hasMessage(
				"When configuring a manager " + "step for remote chunking, the item writer will be automatically "
						+ "set to an instance of ChunkMessageChannelItemWriter. "
						+ "The item writer must not be provided in this case.");
	}

	@Test
	void testManagerStepCreation() {
		// when
		TaskletStep taskletStep = new RemoteChunkingManagerStepBuilder<String, String>("step", this.jobRepository)
			.reader(this.itemReader)
			.transactionManager(this.transactionManager)
			.inputChannel(this.inputChannel)
			.outputChannel(this.outputChannel)
			.build();

		// then
		assertNotNull(taskletStep);
	}

	/*
	 * The following test is to cover setters that override those from parent builders.
	 */
	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void testSetters() throws Exception {
		// when
		DefaultTransactionAttribute transactionAttribute = new DefaultTransactionAttribute();

		Object annotatedListener = new Object();
		MapRetryContextCache retryCache = new MapRetryContextCache();
		RepeatTemplate stepOperations = new RepeatTemplate();
		NoBackOffPolicy backOffPolicy = new NoBackOffPolicy();
		ItemStreamSupport stream = new ItemStreamSupport() {
		};
		StepExecutionListener stepExecutionListener = mock();
		ItemReadListener<String> itemReadListener = mock();
		ItemWriteListener<String> itemWriteListener = mock();
		ChunkListener chunkListener = mock();
		SkipListener<String, String> skipListener = mock();
		RetryListener retryListener = mock();

		when(retryListener.open(any(), any())).thenReturn(true);

		ItemProcessor<String, String> itemProcessor = item -> {
			if (item.equals("b")) {
				throw new Exception("b was found");
			}
			else {
				return item;
			}
		};

		ItemReader<String> itemReader = new ItemReader<>() {

			int count = 0;

			final List<String> items = Arrays.asList("a", "b", "c", "d", "d", "e", "f", "g", "h", "i");

			@Override
			public @Nullable String read() throws Exception {
				if (count == 6) {
					count++;
					throw new IOException("6th item");
				}
				else if (count == 7) {
					count++;
					throw new RuntimeException("7th item");
				}
				else if (count < items.size()) {
					String item = items.get(count++);
					return item;
				}
				else {
					return null;
				}
			}
		};

		TaskletStep taskletStep = new RemoteChunkingManagerStepBuilder<String, String>("step", this.jobRepository)
			.reader(itemReader)
			.readerIsTransactionalQueue()
			.processor(itemProcessor)
			.transactionManager(this.transactionManager)
			.transactionAttribute(transactionAttribute)
			.inputChannel(this.inputChannel)
			.outputChannel(this.outputChannel)
			.listener(annotatedListener)
			.listener(skipListener)
			.listener(chunkListener)
			.listener(stepExecutionListener)
			.listener(itemReadListener)
			.listener(itemWriteListener)
			.listener(retryListener)
			.skip(Exception.class)
			.noSkip(RuntimeException.class)
			.skipLimit(10)
			.retry(IOException.class)
			.noRetry(RuntimeException.class)
			.retryLimit(10)
			.retryContextCache(retryCache)
			.noRollback(Exception.class)
			.startLimit(3)
			.allowStartIfComplete(true)
			.stepOperations(stepOperations)
			.chunk(3)
			.backOffPolicy(backOffPolicy)
			.stream(stream)
			.keyGenerator(Object::hashCode)
			.build();

		JobParameters jobParameters = new JobParameters();
		JobInstance jobInstance = jobRepository.createJobInstance("job1", jobParameters);
		JobExecution jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters,
				new ExecutionContext());
		StepExecution stepExecution = jobRepository.createStepExecution("step1", jobExecution);

		taskletStep.execute(stepExecution);

		// then
		assertNotNull(taskletStep);
		ChunkOrientedTasklet tasklet = (ChunkOrientedTasklet) ReflectionTestUtils.getField(taskletStep, "tasklet");
		SimpleChunkProvider provider = (SimpleChunkProvider) ReflectionTestUtils.getField(tasklet, "chunkProvider");
		SimpleChunkProcessor processor = (SimpleChunkProcessor) ReflectionTestUtils.getField(tasklet, "chunkProcessor");
		ItemWriter itemWriter = (ItemWriter) ReflectionTestUtils.getField(processor, "itemWriter");
		MessagingTemplate messagingTemplate = (MessagingTemplate) ReflectionTestUtils.getField(itemWriter,
				"messagingGateway");
		CompositeItemStream compositeItemStream = (CompositeItemStream) ReflectionTestUtils.getField(taskletStep,
				"stream");

		assertEquals(ReflectionTestUtils.getField(provider, "itemReader"), itemReader);
		assertFalse((Boolean) ReflectionTestUtils.getField(tasklet, "buffering"));
		assertEquals(ReflectionTestUtils.getField(taskletStep, "jobRepository"), this.jobRepository);
		assertEquals(ReflectionTestUtils.getField(taskletStep, "transactionManager"), this.transactionManager);
		assertEquals(ReflectionTestUtils.getField(taskletStep, "transactionAttribute"), transactionAttribute);
		assertEquals(ReflectionTestUtils.getField(itemWriter, "replyChannel"), this.inputChannel);
		assertEquals(ReflectionTestUtils.getField(messagingTemplate, "defaultDestination"), this.outputChannel);
		assertEquals(ReflectionTestUtils.getField(processor, "itemProcessor"), itemProcessor);

		assertEquals((int) ReflectionTestUtils.getField(taskletStep, "startLimit"), 3);
		assertTrue((Boolean) ReflectionTestUtils.getField(taskletStep, "allowStartIfComplete"));
		Object stepOperationsUsed = ReflectionTestUtils.getField(taskletStep, "stepOperations");
		assertEquals(stepOperationsUsed, stepOperations);

		assertEquals(((List) ReflectionTestUtils.getField(compositeItemStream, "streams")).size(), 2);
		assertNotNull(ReflectionTestUtils.getField(processor, "keyGenerator"));

		verify(skipListener, atLeastOnce()).onSkipInProcess(any(), any());
		verify(retryListener, atLeastOnce()).open(any(), any());
		verify(stepExecutionListener, atLeastOnce()).beforeStep(any());
		verify(chunkListener, atLeastOnce()).beforeChunk((ChunkContext) any());
		verify(itemReadListener, atLeastOnce()).beforeRead();
		verify(itemWriteListener, atLeastOnce()).beforeWrite(any());

		assertEquals(stepExecution.getSkipCount(), 2);
		assertEquals(stepExecution.getRollbackCount(), 3);
	}

	@Configuration
	@EnableBatchProcessing
	@EnableJdbcJobRepository
	static class BatchConfiguration {

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
				.addScript("/org/springframework/batch/core/schema-hsqldb.sql")
				.generateUniqueName(true)
				.build();
		}

		@Bean
		JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

	}

}
