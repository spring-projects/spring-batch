/*
 * Copyright 2018 the original author or authors.
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

import org.springframework.batch.core.step.item.SimpleChunkProcessor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.PassThroughItemProcessor;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * Builder for a worker in a remote chunking setup. This builder:
 *
 * <ul>
 *     <li>creates a {@link ChunkProcessorChunkHandler} with the provided
 *     item processor and writer. If no item processor is provided, a
 *     {@link PassThroughItemProcessor} will be used</li>
 *     <li>creates an {@link IntegrationFlow} with the
 *     {@link ChunkProcessorChunkHandler} as a service activator which listens
 *     to incoming requests on <code>inputChannel</code> and sends replies
 *     on <code>outputChannel</code></li>
 * </ul>
 *
 * @param <I> type of input items
 * @param <O> type of output items
 *
 * @since 4.1
 * @author Mahmoud Ben Hassine
 */
public class RemoteChunkingWorkerBuilder<I, O> {

	private static final String SERVICE_ACTIVATOR_METHOD_NAME = "handleChunk";

	private ItemProcessor<I, O> itemProcessor;
	private ItemWriter<O> itemWriter;
	private MessageChannel inputChannel;
	private MessageChannel outputChannel;

	/**
	 * Set the {@link ItemProcessor} to use to process items sent by the master
	 * step.
	 *
	 * @param itemProcessor to use
	 * @return this builder instance for fluent chaining
	 */
	public RemoteChunkingWorkerBuilder<I, O> itemProcessor(ItemProcessor<I, O> itemProcessor) {
		Assert.notNull(itemProcessor, "itemProcessor must not be null");
		this.itemProcessor = itemProcessor;
		return this;
	}

	/**
	 * Set the {@link ItemWriter} to use to write items sent by the master step.
	 *
	 * @param itemWriter to use
	 * @return this builder instance for fluent chaining
	 */
	public RemoteChunkingWorkerBuilder<I, O> itemWriter(ItemWriter<O> itemWriter) {
		Assert.notNull(itemWriter, "itemWriter must not be null");
		this.itemWriter = itemWriter;
		return this;
	}

	/**
	 * Set the input channel on which items sent by the master are received.
	 *
	 * @param inputChannel the input channel
	 * @return this builder instance for fluent chaining
	 */
	public RemoteChunkingWorkerBuilder<I, O> inputChannel(MessageChannel inputChannel) {
		Assert.notNull(inputChannel, "inputChannel must not be null");
		this.inputChannel = inputChannel;
		return this;
	}

	/**
	 * Set the output channel on which replies will be sent to the master step.
	 *
	 * @param outputChannel the output channel
	 * @return this builder instance for fluent chaining
	 */
	public RemoteChunkingWorkerBuilder<I, O> outputChannel(MessageChannel outputChannel) {
		Assert.notNull(outputChannel, "outputChannel must not be null");
		this.outputChannel = outputChannel;
		return this;
	}

	/**
	 * Create an {@link IntegrationFlow} with a {@link ChunkProcessorChunkHandler}
	 * configured as a service activator listening to the input channel and replying
	 * on the output channel.
	 *
	 * @return the integration flow
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public IntegrationFlow build() {
		Assert.notNull(this.itemWriter, "An ItemWriter must be provided");
		Assert.notNull(this.inputChannel, "An InputChannel must be provided");
		Assert.notNull(this.outputChannel, "An OutputChannel must be provided");

		if(this.itemProcessor == null) {
			this.itemProcessor = new PassThroughItemProcessor();
		}
		SimpleChunkProcessor<I, O> chunkProcessor = new SimpleChunkProcessor<>(this.itemProcessor, this.itemWriter);

		ChunkProcessorChunkHandler<I> chunkProcessorChunkHandler = new ChunkProcessorChunkHandler<>();
		chunkProcessorChunkHandler.setChunkProcessor(chunkProcessor);

		return IntegrationFlows
				.from(this.inputChannel)
				.handle(chunkProcessorChunkHandler, SERVICE_ACTIVATOR_METHOD_NAME)
				.channel(this.outputChannel)
				.get();
	}

}
