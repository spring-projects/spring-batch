/*
 * Copyright 2026-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.item.ChunkOrientedStep;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.Assert;

/**
 * Builder for the manager side of a remote chunking setup. The manager step is a regular
 * {@link ChunkOrientedStep}, whose item writer is a {@link ChunkMessageChannelItemWriter}
 * that sends chunks to remote workers and waits for their replies.
 *
 * <p>
 * This builder holds the settings that are specific to remote chunking (the channels, the
 * messaging template, the maximum number of wait timeouts and the throttle limit). Once
 * those are set, calling {@link #chunk(int)} returns a {@link ChunkOrientedStepBuilder}
 * on which everything else (reader, processor, transaction manager, listeners, retry and
 * skip policies, etc) is configured as for any other chunk-oriented step:
 * </p>
 *
 * <pre class="code">
 * Step managerStep = new RemoteChunkingManagerBuilder("managerStep", jobRepository)
 *     .outputChannel(requests)
 *     .inputChannel(replies)
 *     .&lt;Integer, Integer&gt; chunk(100)
 *     .reader(itemReader)
 *     .transactionManager(transactionManager)
 *     .build();
 * </pre>
 *
 * <p>
 * If no {@code messagingTemplate} is provided through
 * {@link #messagingTemplate(MessagingTemplate)}, this builder will create one and set its
 * default channel to the {@code outputChannel} provided through
 * {@link #outputChannel(MessageChannel)}. If a {@code messagingTemplate} is provided, it
 * is assumed that it is fully configured and that its default channel is set to an output
 * channel on which requests to workers will be sent.
 * </p>
 *
 * <p>
 * Please note that:
 * </p>
 * <ul>
 * <li>The item writer of the step is set by this builder and must not be changed on the
 * {@link ChunkOrientedStepBuilder} returned by {@link #chunk(int)}.</li>
 * <li>Only {@link #chunk(int)} configures a remote chunking manager step. Other methods
 * inherited from {@link StepBuilder} that create steps are not meant to be used with this
 * builder.</li>
 * <li>As for any {@link ChunkOrientedStepBuilder}, the transaction manager defaults to a
 * resourceless one. It should be set explicitly if the item reader of the manager step
 * needs a transactional resource.</li>
 * </ul>
 *
 * @author Mahmoud Ben Hassine
 * @since 6.1
 * @see RemoteChunkingWorkerBuilder
 */
public class RemoteChunkingManagerBuilder extends StepBuilder {

	private static final int DEFAULT_MAX_WAIT_TIMEOUTS = 40;

	private static final long DEFAULT_THROTTLE_LIMIT = 6;

	private @Nullable MessagingTemplate messagingTemplate;

	private @Nullable SubscribableChannel inputChannel;

	private @Nullable MessageChannel outputChannel;

	private int maxWaitTimeouts = DEFAULT_MAX_WAIT_TIMEOUTS;

	private long throttleLimit = DEFAULT_THROTTLE_LIMIT;

	/**
	 * Create a new {@link RemoteChunkingManagerBuilder}.
	 * @param name name of the manager step
	 * @param jobRepository the job repository the step should report to
	 */
	public RemoteChunkingManagerBuilder(String name, JobRepository jobRepository) {
		super(name, jobRepository);
	}

	/**
	 * Set the input channel on which replies from workers will be received. The provided
	 * input channel will be set as a reply channel on the
	 * {@link ChunkMessageChannelItemWriter} created by this builder.
	 * @param inputChannel the input channel
	 * @return this builder instance for fluent chaining
	 * @see ChunkMessageChannelItemWriter#setReplyChannel
	 */
	public RemoteChunkingManagerBuilder inputChannel(SubscribableChannel inputChannel) {
		Assert.notNull(inputChannel, "inputChannel must not be null");
		this.inputChannel = inputChannel;
		return this;
	}

	/**
	 * Set the output channel on which requests to workers will be sent. By using this
	 * setter, a default messaging template will be created and the output channel will be
	 * set as its default channel.
	 * <p>
	 * Use either this setter or {@link #messagingTemplate(MessagingTemplate)} to provide
	 * a fully configured messaging template.
	 * </p>
	 * @param outputChannel the output channel
	 * @return this builder instance for fluent chaining
	 * @see #messagingTemplate(MessagingTemplate)
	 */
	public RemoteChunkingManagerBuilder outputChannel(MessageChannel outputChannel) {
		Assert.notNull(outputChannel, "outputChannel must not be null");
		this.outputChannel = outputChannel;
		return this;
	}

	/**
	 * Set the {@link MessagingTemplate} to use to send data to workers. <strong>The
	 * default channel of the messaging template must be set</strong>.
	 * <p>
	 * Use either this setter to provide a fully configured messaging template or provide
	 * an output channel through {@link #outputChannel(MessageChannel)} and a default
	 * messaging template will be created.
	 * </p>
	 * @param messagingTemplate the messaging template to use
	 * @return this builder instance for fluent chaining
	 * @see #outputChannel(MessageChannel)
	 */
	public RemoteChunkingManagerBuilder messagingTemplate(MessagingTemplate messagingTemplate) {
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
	public RemoteChunkingManagerBuilder maxWaitTimeouts(int maxWaitTimeouts) {
		Assert.isTrue(maxWaitTimeouts > 0, "maxWaitTimeouts must be greater than zero");
		this.maxWaitTimeouts = maxWaitTimeouts;
		return this;
	}

	/**
	 * Set the throttle limit. This limits the number of pending requests for chunk
	 * processing to avoid overwhelming the receivers. Defaults to 6.
	 * @param throttleLimit the throttle limit to set
	 * @return this builder instance for fluent chaining
	 * @see ChunkMessageChannelItemWriter#setThrottleLimit(long)
	 */
	public RemoteChunkingManagerBuilder throttleLimit(long throttleLimit) {
		Assert.isTrue(throttleLimit > 0, "throttleLimit must be greater than zero");
		this.throttleLimit = throttleLimit;
		return this;
	}

	/**
	 * Create a {@link ChunkOrientedStepBuilder} for a manager step with the given chunk
	 * size. The item writer of the returned builder is a
	 * {@link ChunkMessageChannelItemWriter} configured with the settings of this builder,
	 * and must not be changed.
	 * @param chunkSize the chunk size (commit interval)
	 * @param <I> the type of item to be processed as input
	 * @param <O> the type of item to be output
	 * @return a {@link ChunkOrientedStepBuilder} for method chaining
	 * @throws IllegalArgumentException if no input channel is provided
	 * @throws IllegalStateException if both an output channel and a messaging template
	 * are provided, or if none of them is
	 */
	@Override
	public <I, O> ChunkOrientedStepBuilder<I, O> chunk(int chunkSize) {
		SubscribableChannel inputChannel = this.inputChannel;
		Assert.notNull(inputChannel, "An InputChannel must be provided");
		MessagingTemplate messagingTemplate = this.messagingTemplate;
		MessageChannel outputChannel = this.outputChannel;
		Assert.state(outputChannel == null || messagingTemplate == null,
				"You must specify either an outputChannel or a messagingTemplate but not both.");
		Assert.state(outputChannel != null || messagingTemplate != null,
				"You must specify either an outputChannel or a messagingTemplate.");

		// configure messaging template
		if (messagingTemplate == null) {
			messagingTemplate = new MessagingTemplate();
			messagingTemplate.setDefaultChannel(outputChannel);
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("No messagingTemplate was provided, using a default one");
			}
		}

		// configure item writer
		ChunkMessageChannelItemWriter<O> chunkMessageChannelItemWriter = new ChunkMessageChannelItemWriter<>();
		chunkMessageChannelItemWriter.setMessagingOperations(messagingTemplate);
		chunkMessageChannelItemWriter.setMaxWaitTimeouts(this.maxWaitTimeouts);
		chunkMessageChannelItemWriter.setThrottleLimit(this.throttleLimit);
		chunkMessageChannelItemWriter.setReplyChannel(inputChannel);

		return super.<I, O>chunk(chunkSize).writer(chunkMessageChannelItemWriter);
	}

}
