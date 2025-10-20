/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.batch.integration.support;

import java.util.function.Consumer;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;

/**
 * An {@link ItemWriter} implementation that sends items as messages to a message channel
 * using a {@link MessagingTemplate}. This item writer enables SEDA (Staged Event-Driven
 * Architecture) patterns in Spring Batch jobs by decoupling item production from item
 * consumption through messaging.
 *
 * @param <T> the type of items to be written
 * @author Mahmoud Ben Hassine
 * @since 6.0.0
 */
public class MessageChannelItemWriter<T> implements ItemWriter<T> {

	private final MessagingTemplate messagingTemplate;

	private MessageChannel messageChannel;

	/**
	 * Create a new {@link MessageChannelItemWriter} instance. Messages will be sent to
	 * the default destination of the provided {@link MessagingTemplate} which must not be
	 * null.
	 * @param messagingGateway the messaging template to use for sending messages
	 */
	public MessageChannelItemWriter(MessagingTemplate messagingGateway) {
		this.messagingTemplate = messagingGateway;
		MessageChannel defaultDestination = messagingGateway.getDefaultDestination();
		Assert.notNull(defaultDestination, "MessagingTemplate must have a default destination configured");
		this.messageChannel = defaultDestination;
	}

	/**
	 * Create a new {@link MessageChannelItemWriter} instance. Messages will be sent to
	 * the provided message channel.
	 * @param messagingTemplate the messaging template to use for sending messages
	 * @param messageChannel the message channel to send messages to
	 */
	public MessageChannelItemWriter(MessagingTemplate messagingTemplate, MessageChannel messageChannel) {
		this.messagingTemplate = messagingTemplate;
		this.messageChannel = messageChannel;
	}

	/**
	 * Set the target message channel.
	 * @param messageChannel the message channel to send messages to
	 */
	public void setMessageChannel(MessageChannel messageChannel) {
		this.messageChannel = messageChannel;
	}

	@Override
	public void write(Chunk<? extends T> items) throws Exception {
		if (!items.isEmpty()) {
			items.forEach((Consumer<T>) t -> this.messagingTemplate.send(this.messageChannel, new GenericMessage<>(t)));
		}
	}

}
