/*
 * Copyright 2018-2019 the original author or authors.
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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.item.ChunkOrientedTasklet;
import org.springframework.batch.core.step.item.SimpleChunkProcessor;
import org.springframework.batch.core.step.item.SimpleChunkProvider;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.CompositeItemStream;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.lang.Nullable;
import org.springframework.messaging.PollableChannel;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.MapRetryContextCache;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Mahmoud Ben Hassine
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {RemoteChunkingManagerStepBuilderTest.BatchConfiguration.class})
public class RemoteChunkingManagerStepBuilderTest {

	@Autowired
	private JobRepository jobRepository;
	@Autowired
	private PlatformTransactionManager transactionManager;

	private PollableChannel inputChannel = new QueueChannel();
	private DirectChannel outputChannel = new DirectChannel();
	private ItemReader<String> itemReader = new ListItemReader<>(Arrays.asList("a", "b", "c"));

	@Test
	public void inputChannelMustNotBeNull() {
		// given
		final RemoteChunkingManagerStepBuilder<String, String> builder = new RemoteChunkingManagerStepBuilder<String, String>("step")
				.inputChannel(null);

		// when
		final Exception expectedException = Assert.assertThrows(IllegalArgumentException.class, builder::build);

		// then
		assertThat(expectedException).hasMessage("inputChannel must not be null");
	}

	@Test
	public void outputChannelMustNotBeNull() {
		// given
		final RemoteChunkingManagerStepBuilder<String, String> builder = new RemoteChunkingManagerStepBuilder<String, String>("step")
				.outputChannel(null);

		// when
		final Exception expectedException = Assert.assertThrows(IllegalArgumentException.class, builder::build);

		// then
		assertThat(expectedException).hasMessage("outputChannel must not be null");
	}

	@Test
	public void messagingTemplateMustNotBeNull() {
		// given
		final RemoteChunkingManagerStepBuilder<String, String> builder = new RemoteChunkingManagerStepBuilder<String, String>("step")
				.messagingTemplate(null);

		// when
		final Exception expectedException = Assert.assertThrows(IllegalArgumentException.class, builder::build);

		// then
		assertThat(expectedException).hasMessage("messagingTemplate must not be null");
	}

	@Test
	public void maxWaitTimeoutsMustBeGreaterThanZero() {
		// given
		final RemoteChunkingManagerStepBuilder<String, String> builder = new RemoteChunkingManagerStepBuilder<String, String>("step")
				.maxWaitTimeouts(-1);

		// when
		final Exception expectedException = Assert.assertThrows(IllegalArgumentException.class, builder::build);

		// then
		assertThat(expectedException).hasMessage("maxWaitTimeouts must be greater than zero");
	}

	@Test
	public void throttleLimitMustNotBeGreaterThanZero() {
		// given
		final RemoteChunkingManagerStepBuilder<String, String> builder = new RemoteChunkingManagerStepBuilder<String, String>("step")
				.throttleLimit(-1L);

		// when
		final Exception expectedException = Assert.assertThrows(IllegalArgumentException.class, builder::build);

		// then
		assertThat(expectedException).hasMessage("throttleLimit must be greater than zero");
	}

	@Test
	public void testMandatoryInputChannel() {
		// given
		RemoteChunkingManagerStepBuilder<String, String> builder = new RemoteChunkingManagerStepBuilder<>("step");

		// when
		final Exception expectedException = Assert.assertThrows(IllegalArgumentException.class, builder::build);

		// then
		assertThat(expectedException).hasMessage("An InputChannel must be provided");
	}

	@Test
	public void eitherOutputChannelOrMessagingTemplateMustBeProvided() {
		// given
		RemoteChunkingManagerStepBuilder<String, String> builder = new RemoteChunkingManagerStepBuilder<String, String>("step")
				.inputChannel(this.inputChannel)
				.outputChannel(new DirectChannel())
				.messagingTemplate(new MessagingTemplate());

		// when
		final Exception expectedException = Assert.assertThrows(IllegalStateException.class, builder::build);

		// then
		assertThat(expectedException).hasMessage("You must specify either an outputChannel or a messagingTemplate but not both.");
	}

	@Test
	public void testUnsupportedOperationExceptionWhenSpecifyingAnItemWriter() {
		// given
		final RemoteChunkingManagerStepBuilder<String, String> builder = new RemoteChunkingManagerStepBuilder<String, String>("step")
				.reader(this.itemReader)
				.writer(items -> { })
				.repository(this.jobRepository)
				.transactionManager(this.transactionManager)
				.inputChannel(this.inputChannel)
				.outputChannel(this.outputChannel);

		// when
		final Exception expectedException = Assert.assertThrows(UnsupportedOperationException.class, builder::build);

		// then
		assertThat(expectedException).hasMessage("When configuring a manager " +
				"step for remote chunking, the item writer will be automatically " +
				"set to an instance of ChunkMessageChannelItemWriter. " +
				"The item writer must not be provided in this case.");
	}

	@Test
	public void testManagerStepCreation() {
		// when
		TaskletStep taskletStep = new RemoteChunkingManagerStepBuilder<String, String>("step")
				.reader(this.itemReader)
				.repository(this.jobRepository)
				.transactionManager(this.transactionManager)
				.inputChannel(this.inputChannel)
				.outputChannel(this.outputChannel)
				.build();

		// then
		Assert.assertNotNull(taskletStep);
	}

	/*
	 * The following test is to cover setters that override those from parent builders.
	 */
	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void testSetters() throws Exception {
		// when
		DefaultTransactionAttribute transactionAttribute = new DefaultTransactionAttribute();

		Object annotatedListener = new Object();
		MapRetryContextCache retryCache = new MapRetryContextCache();
		RepeatTemplate stepOperations = new RepeatTemplate();
		NoBackOffPolicy backOffPolicy = new NoBackOffPolicy();
		ItemStreamSupport stream = new ItemStreamSupport() {
		};
		StepExecutionListener stepExecutionListener = mock(StepExecutionListener.class);
		ItemReadListener<String> itemReadListener = mock(ItemReadListener.class);
		ItemWriteListener<String> itemWriteListener = mock(ItemWriteListener.class);
		ChunkListener chunkListener = mock(ChunkListener.class);
		SkipListener<String, String> skipListener = mock(SkipListener.class);
		RetryListener retryListener = mock(RetryListener.class);

		when(retryListener.open(any(), any())).thenReturn(true);

		ItemProcessor<String, String> itemProcessor = item -> {
			System.out.println("processing item " + item);
			if(item.equals("b")) {
				throw new Exception("b was found");
			}
			else {
				return item;
			}
		};

		ItemReader<String> itemReader = new ItemReader<String>() {

			int count = 0;
			List<String> items = Arrays.asList("a", "b", "c", "d", "d", "e", "f", "g", "h", "i");

			@Nullable
			@Override
			public String read() throws Exception {
				System.out.println(">> count == " + count);
				if(count == 6) {
					count++;
					throw new IOException("6th item");
				}
				else if(count == 7) {
					count++;
					throw new RuntimeException("7th item");
				}
				else if(count < items.size()){
					String item = items.get(count++);
					System.out.println(">> item read was " + item);
					return item;
				}
				else {
					return null;
				}
			}
		};

		TaskletStep taskletStep = new RemoteChunkingManagerStepBuilder<String, String>("step")
				.reader(itemReader)
				.readerIsTransactionalQueue()
				.processor(itemProcessor)
				.repository(this.jobRepository)
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

		JobExecution jobExecution = this.jobRepository.createJobExecution("job1", new JobParameters());
		StepExecution stepExecution = new StepExecution("step1", jobExecution);
		this.jobRepository.add(stepExecution);

		taskletStep.execute(stepExecution);

		// then
		Assert.assertNotNull(taskletStep);
		ChunkOrientedTasklet tasklet = (ChunkOrientedTasklet) ReflectionTestUtils.getField(taskletStep, "tasklet");
		SimpleChunkProvider provider = (SimpleChunkProvider) ReflectionTestUtils.getField(tasklet, "chunkProvider");
		SimpleChunkProcessor processor = (SimpleChunkProcessor) ReflectionTestUtils.getField(tasklet, "chunkProcessor");
		ItemWriter itemWriter = (ItemWriter) ReflectionTestUtils.getField(processor, "itemWriter");
		MessagingTemplate messagingTemplate = (MessagingTemplate) ReflectionTestUtils.getField(itemWriter, "messagingGateway");
		CompositeItemStream compositeItemStream = (CompositeItemStream) ReflectionTestUtils.getField(taskletStep, "stream");

		Assert.assertEquals(ReflectionTestUtils.getField(provider, "itemReader"), itemReader);
		Assert.assertFalse((Boolean) ReflectionTestUtils.getField(tasklet, "buffering"));
		Assert.assertEquals(ReflectionTestUtils.getField(taskletStep, "jobRepository"), this.jobRepository);
		Assert.assertEquals(ReflectionTestUtils.getField(taskletStep, "transactionManager"), this.transactionManager);
		Assert.assertEquals(ReflectionTestUtils.getField(taskletStep, "transactionAttribute"), transactionAttribute);
		Assert.assertEquals(ReflectionTestUtils.getField(itemWriter, "replyChannel"), this.inputChannel);
		Assert.assertEquals(ReflectionTestUtils.getField(messagingTemplate, "defaultDestination"), this.outputChannel);
		Assert.assertEquals(ReflectionTestUtils.getField(processor, "itemProcessor"), itemProcessor);

		Assert.assertEquals((int) ReflectionTestUtils.getField(taskletStep, "startLimit"), 3);
		Assert.assertTrue((Boolean) ReflectionTestUtils.getField(taskletStep, "allowStartIfComplete"));
		Object stepOperationsUsed = ReflectionTestUtils.getField(taskletStep, "stepOperations");
		Assert.assertEquals(stepOperationsUsed, stepOperations);

		Assert.assertEquals(((List)ReflectionTestUtils.getField(compositeItemStream, "streams")).size(), 2);
		Assert.assertNotNull(ReflectionTestUtils.getField(processor, "keyGenerator"));

		verify(skipListener, atLeastOnce()).onSkipInProcess(any(), any());
		verify(retryListener, atLeastOnce()).open(any(), any());
		verify(stepExecutionListener, atLeastOnce()).beforeStep(any());
		verify(chunkListener, atLeastOnce()).beforeChunk(any());
		verify(itemReadListener, atLeastOnce()).beforeRead();
		verify(itemWriteListener, atLeastOnce()).beforeWrite(any());

		Assert.assertEquals(stepExecution.getSkipCount(), 2);
		Assert.assertEquals(stepExecution.getRollbackCount(), 3);
	}

	@Configuration
	@EnableBatchProcessing
	public static class BatchConfiguration {

	}
}
