/*
 * Copyright 2019-2025 the original author or authors.
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

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.FaultTolerantStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.item.KeyGenerator;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.exception.ExceptionHandler;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.policy.RetryContextCache;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.util.Assert;

/**
 * Builder for a manager step in a remote chunking setup. This builder creates and sets a
 * {@link ChunkMessageChannelItemWriter} on the manager step.
 *
 * <p>
 * If no {@code messagingTemplate} is provided through
 * {@link RemoteChunkingManagerStepBuilder#messagingTemplate(MessagingTemplate)}, this
 * builder will create one and set its default channel to the {@code outputChannel}
 * provided through
 * {@link RemoteChunkingManagerStepBuilder#outputChannel(MessageChannel)}.
 * </p>
 *
 * <p>
 * If a {@code messagingTemplate} is provided, it is assumed that it is fully configured
 * and that its default channel is set to an output channel on which requests to workers
 * will be sent.
 * </p>
 *
 * @param <I> type of input items
 * @param <O> type of output items
 * @since 4.2
 * @author Mahmoud Ben Hassine
 */
public class RemoteChunkingManagerStepBuilder<I, O> extends FaultTolerantStepBuilder<I, O> {

	private MessagingTemplate messagingTemplate;

	private PollableChannel inputChannel;

	private MessageChannel outputChannel;

	private final int DEFAULT_MAX_WAIT_TIMEOUTS = 40;

	private static final long DEFAULT_THROTTLE_LIMIT = 6;

	private int maxWaitTimeouts = DEFAULT_MAX_WAIT_TIMEOUTS;

	private long throttleLimit = DEFAULT_THROTTLE_LIMIT;

	/**
	 * Create a new {@link RemoteChunkingManagerStepBuilder}.
	 * @param stepName name of the manager step
	 * @param jobRepository the job repository the step should report to
	 * @since 5.0
	 */
	public RemoteChunkingManagerStepBuilder(String stepName, JobRepository jobRepository) {
		super(new StepBuilder(stepName, jobRepository));
	}

	/**
	 * Set the input channel on which replies from workers will be received. The provided
	 * input channel will be set as a reply channel on the
	 * {@link ChunkMessageChannelItemWriter} created by this builder.
	 * @param inputChannel the input channel
	 * @return this builder instance for fluent chaining
	 *
	 * @see ChunkMessageChannelItemWriter#setReplyChannel
	 */
	public RemoteChunkingManagerStepBuilder<I, O> inputChannel(PollableChannel inputChannel) {
		Assert.notNull(inputChannel, "inputChannel must not be null");
		this.inputChannel = inputChannel;
		return this;
	}

	/**
	 * Set the output channel on which requests to workers will be sent. By using this
	 * setter, a default messaging template will be created and the output channel will be
	 * set as its default channel.
	 * <p>
	 * Use either this setter or
	 * {@link RemoteChunkingManagerStepBuilder#messagingTemplate(MessagingTemplate)} to
	 * provide a fully configured messaging template.
	 * </p>
	 * @param outputChannel the output channel.
	 * @return this builder instance for fluent chaining
	 *
	 * @see RemoteChunkingManagerStepBuilder#messagingTemplate(MessagingTemplate)
	 */
	public RemoteChunkingManagerStepBuilder<I, O> outputChannel(MessageChannel outputChannel) {
		Assert.notNull(outputChannel, "outputChannel must not be null");
		this.outputChannel = outputChannel;
		return this;
	}

	/**
	 * Set the {@link MessagingTemplate} to use to send data to workers. <strong>The
	 * default channel of the messaging template must be set</strong>.
	 * <p>
	 * Use either this setter to provide a fully configured messaging template or provide
	 * an output channel through
	 * {@link RemoteChunkingManagerStepBuilder#outputChannel(MessageChannel)} and a
	 * default messaging template will be created.
	 * </p>
	 * @param messagingTemplate the messaging template to use
	 * @return this builder instance for fluent chaining
	 * @see RemoteChunkingManagerStepBuilder#outputChannel(MessageChannel)
	 */
	public RemoteChunkingManagerStepBuilder<I, O> messagingTemplate(MessagingTemplate messagingTemplate) {
		Assert.notNull(messagingTemplate, "messagingTemplate must not be null");
		this.messagingTemplate = messagingTemplate;
		return this;
	}

	/**
	 * The maximum number of times to wait at the end of a step for a non-null result from
	 * the remote workers. This is a multiplier on the receive timeout set separately on
	 * the gateway. The ideal value is a compromise between allowing slow workers time to
	 * finish, and responsiveness if there is a dead worker. Defaults to 40.
	 * @param maxWaitTimeouts the maximum number of wait timeouts
	 * @return this builder instance for fluent chaining
	 * @see ChunkMessageChannelItemWriter#setMaxWaitTimeouts(int)
	 */
	public RemoteChunkingManagerStepBuilder<I, O> maxWaitTimeouts(int maxWaitTimeouts) {
		Assert.isTrue(maxWaitTimeouts > 0, "maxWaitTimeouts must be greater than zero");
		this.maxWaitTimeouts = maxWaitTimeouts;
		return this;
	}

	/**
	 * Public setter for the throttle limit. This limits the number of pending requests
	 * for chunk processing to avoid overwhelming the receivers.
	 * @param throttleLimit the throttle limit to set
	 * @return this builder instance for fluent chaining
	 * @see ChunkMessageChannelItemWriter#setThrottleLimit(long)
	 */
	public RemoteChunkingManagerStepBuilder<I, O> throttleLimit(long throttleLimit) {
		Assert.isTrue(throttleLimit > 0, "throttleLimit must be greater than zero");
		this.throttleLimit = throttleLimit;
		return this;
	}

	/**
	 * Build a manager {@link TaskletStep}.
	 * @return the configured manager step
	 * @see RemoteChunkHandlerFactoryBean
	 */
	@Override
	public TaskletStep build() {
		Assert.notNull(this.inputChannel, "An InputChannel must be provided");
		Assert.state(this.outputChannel == null || this.messagingTemplate == null,
				"You must specify either an outputChannel or a messagingTemplate but not both.");

		// configure messaging template
		if (this.messagingTemplate == null) {
			this.messagingTemplate = new MessagingTemplate();
			this.messagingTemplate.setDefaultChannel(this.outputChannel);
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("No messagingTemplate was provided, using a default one");
			}
		}

		// configure item writer
		ChunkMessageChannelItemWriter<O> chunkMessageChannelItemWriter = new ChunkMessageChannelItemWriter<>();
		chunkMessageChannelItemWriter.setMessagingOperations(this.messagingTemplate);
		chunkMessageChannelItemWriter.setMaxWaitTimeouts(this.maxWaitTimeouts);
		chunkMessageChannelItemWriter.setThrottleLimit(this.throttleLimit);
		chunkMessageChannelItemWriter.setReplyChannel(this.inputChannel);
		super.writer(chunkMessageChannelItemWriter);

		return super.build();
	}

	/*
	 * The following methods override those from parent builders and return the current
	 * builder type. FIXME: Change parent builders to be generic and return current
	 * builder type in each method.
	 */

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> reader(ItemReader<? extends I> reader) {
		super.reader(reader);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> transactionManager(PlatformTransactionManager transactionManager) {
		super.transactionManager(transactionManager);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> listener(Object listener) {
		super.listener(listener);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> listener(SkipListener<? super I, ? super O> listener) {
		super.listener(listener);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> listener(ChunkListener listener) {
		super.listener(listener);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> transactionAttribute(TransactionAttribute transactionAttribute) {
		super.transactionAttribute(transactionAttribute);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> listener(org.springframework.retry.RetryListener listener) {
		super.listener(listener);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> keyGenerator(KeyGenerator keyGenerator) {
		super.keyGenerator(keyGenerator);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> retryLimit(int retryLimit) {
		super.retryLimit(retryLimit);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> retryPolicy(RetryPolicy retryPolicy) {
		super.retryPolicy(retryPolicy);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> backOffPolicy(BackOffPolicy backOffPolicy) {
		super.backOffPolicy(backOffPolicy);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> retryContextCache(RetryContextCache retryContextCache) {
		super.retryContextCache(retryContextCache);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> skipLimit(int skipLimit) {
		super.skipLimit(skipLimit);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> noSkip(Class<? extends Throwable> type) {
		super.noSkip(type);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> skip(Class<? extends Throwable> type) {
		super.skip(type);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> skipPolicy(SkipPolicy skipPolicy) {
		super.skipPolicy(skipPolicy);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> noRollback(Class<? extends Throwable> type) {
		super.noRollback(type);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> noRetry(Class<? extends Throwable> type) {
		super.noRetry(type);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> retry(Class<? extends Throwable> type) {
		super.retry(type);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> stream(ItemStream stream) {
		super.stream(stream);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> chunk(int chunkSize) {
		super.chunk(chunkSize);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> chunk(CompletionPolicy completionPolicy) {
		super.chunk(completionPolicy);
		return this;
	}

	/**
	 * This method will throw a {@link UnsupportedOperationException} since the item
	 * writer of the manager step in a remote chunking setup will be automatically set to
	 * an instance of {@link ChunkMessageChannelItemWriter}.
	 * <p>
	 * When building a manager step for remote chunking, no item writer must be provided.
	 * @throws UnsupportedOperationException if an item writer is provided
	 * @see ChunkMessageChannelItemWriter
	 * @see RemoteChunkHandlerFactoryBean#setChunkWriter(ItemWriter)
	 */
	@Override
	public RemoteChunkingManagerStepBuilder<I, O> writer(ItemWriter<? super O> writer)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException(
				"When configuring a manager step " + "for remote chunking, the item writer will be automatically set "
						+ "to an instance of ChunkMessageChannelItemWriter. The item writer "
						+ "must not be provided in this case.");
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> readerIsTransactionalQueue() {
		super.readerIsTransactionalQueue();
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> listener(ItemReadListener<? super I> listener) {
		super.listener(listener);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> listener(ItemWriteListener<? super O> listener) {
		super.listener(listener);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> chunkOperations(RepeatOperations repeatTemplate) {
		super.chunkOperations(repeatTemplate);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> exceptionHandler(ExceptionHandler exceptionHandler) {
		super.exceptionHandler(exceptionHandler);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> stepOperations(RepeatOperations repeatTemplate) {
		super.stepOperations(repeatTemplate);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> startLimit(int startLimit) {
		super.startLimit(startLimit);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> listener(StepExecutionListener listener) {
		super.listener(listener);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> allowStartIfComplete(boolean allowStartIfComplete) {
		super.allowStartIfComplete(allowStartIfComplete);
		return this;
	}

	@Override
	public RemoteChunkingManagerStepBuilder<I, O> processor(ItemProcessor<? super I, ? extends O> itemProcessor) {
		super.processor(itemProcessor);
		return this;
	}

}
