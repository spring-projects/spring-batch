/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.integration.chunk;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.listener.ChunkListenerSupport;
import org.springframework.batch.core.listener.CompositeItemReadListener;
import org.springframework.batch.core.listener.CompositeItemWriteListener;
import org.springframework.batch.core.listener.SkipListenerSupport;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.exception.DefaultExceptionHandler;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.listener.RetryListenerSupport;
import org.springframework.retry.policy.MapRetryContextCache;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

/**
 * @author Mahmoud Ben Hassine
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {RemoteChunkingMasterStepBuilderTest.BatchConfiguration.class})
public class RemoteChunkingMasterStepBuilderTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

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
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("inputChannel must not be null");

		// when
		TaskletStep step = new RemoteChunkingMasterStepBuilder<String, String>("step")
				.inputChannel(null)
				.build();

		// then
		// expected exception
	}

	@Test
	public void outputChannelMustNotBeNull() {
		// given
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("outputChannel must not be null");

		// when
		TaskletStep step = new RemoteChunkingMasterStepBuilder<String, String>("step")
				.outputChannel(null)
				.build();

		// then
		// expected exception
	}

	@Test
	public void messagingTemplateMustNotBeNull() {
		// given
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("messagingTemplate must not be null");

		// when
		TaskletStep step = new RemoteChunkingMasterStepBuilder<String, String>("step")
				.messagingTemplate(null)
				.build();

		// then
		// expected exception
	}

	@Test
	public void maxWaitTimeoutsMustBeGreaterThanZero() {
		// given
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("maxWaitTimeouts must be greater than zero");

		// when
		TaskletStep step = new RemoteChunkingMasterStepBuilder<String, String>("step")
				.maxWaitTimeouts(-1)
				.build();

		// then
		// expected exception
	}

	@Test
	public void throttleLimitMustNotBeGreaterThanZero() {
		// given
		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("throttleLimit must be greater than zero");

		// when
		TaskletStep step = new RemoteChunkingMasterStepBuilder<String, String>("step")
				.throttleLimit(-1L)
				.build();

		// then
		// expected exception
	}

	@Test
	public void testMandatoryInputChannel() {
		// given
		RemoteChunkingMasterStepBuilder<String, String> builder = new RemoteChunkingMasterStepBuilder<>("step");

		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("An InputChannel must be provided");

		// when
		TaskletStep step = builder.build();

		// then
		// expected exception
	}

	@Test
	public void testMandatoryOutputChannel() {
		// given
		RemoteChunkingMasterStepBuilder<String, String> builder = new RemoteChunkingMasterStepBuilder<String, String>("step")
				.inputChannel(this.inputChannel);

		this.expectedException.expect(IllegalArgumentException.class);
		this.expectedException.expectMessage("An OutputChannel must be provided");

		// when
		TaskletStep step = builder.build();

		// then
		// expected exception
	}

	@Test
	public void testUnsupportedOperationExceptionWhenSpecifyingAnItemWriter() {
		// given
		this.expectedException.expect(UnsupportedOperationException.class);
		this.expectedException.expectMessage("When configuring a master " +
				"step for remote chunking, the item writer will be automatically " +
				"set to an instance of ChunkMessageChannelItemWriter. " +
				"The item writer must not be provided in this case.");

		// when
		TaskletStep step = new RemoteChunkingMasterStepBuilder<String, String>("step")
				.reader(this.itemReader)
				.writer(items -> { })
				.repository(this.jobRepository)
				.transactionManager(this.transactionManager)
				.inputChannel(this.inputChannel)
				.outputChannel(this.outputChannel)
				.build();

		// then
		// expected exception
	}

	@Test
	public void testMasterStepCreation() {
		// when
		TaskletStep taskletStep = new RemoteChunkingMasterStepBuilder<String, String>("step")
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
	public void testSetters() {
		// when
		TaskletStep taskletStep = new RemoteChunkingMasterStepBuilder<String, String>("step")
				.reader(this.itemReader)
				.readerIsTransactionalQueue()
				.repository(this.jobRepository)
				.transactionManager(this.transactionManager)
				.transactionAttribute(new DefaultTransactionAttribute())
				.inputChannel(this.inputChannel)
				.outputChannel(this.outputChannel)
				.listener(new Object())
				.listener(new SkipListenerSupport<>())
				.listener(new ChunkListenerSupport())
				.listener(new StepExecutionListenerSupport())
				.listener(new CompositeItemReadListener<>())
				.listener(new CompositeItemWriteListener<>())
				.listener(new RetryListenerSupport())
				.skip(Exception.class)
				.noSkip(RuntimeException.class)
				.skipLimit(10)
				.retry(Exception.class)
				.noRetry(RuntimeException.class)
				.retryLimit(10)
				.retryContextCache(new MapRetryContextCache())
				.noRollback(Exception.class)
				.chunk(10)
				.startLimit(3)
				.allowStartIfComplete(true)
				.exceptionHandler(new DefaultExceptionHandler())
				.stepOperations(new RepeatTemplate())
				.chunkOperations(new RepeatTemplate())
				.backOffPolicy(new NoBackOffPolicy())
				.stream(new ItemStreamSupport() {})
				.keyGenerator(Object::hashCode)
				.build();

		// then
		Assert.assertNotNull(taskletStep);
	}

	@Configuration
	@EnableBatchProcessing
	public static class BatchConfiguration {

	}
}
