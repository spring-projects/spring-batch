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

import org.jspecify.annotations.Nullable;

import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * An {@link ItemReader} implementation that receives items as messages from a message
 * channel using a {@link MessagingTemplate}. This item reader enables SEDA (Staged
 * Event-Driven Architecture) patterns in Spring Batch jobs by decoupling item production
 * from item consumption through messaging.
 *
 * @param <T> the type of items to be read
 * @author Mahmoud Ben Hassine
 * @since 6.0.0
 */
public class MessageChannelItemReader<T> implements ItemReader<T> {

	private final MessagingTemplate messagingTemplate;

	private final Class<T> targetType;

	private MessageChannel messageChannel;

	/**
	 * Create a new {@link MessageChannelItemReader} instance. Messages will be read from
	 * the default destination of the provided {@link MessagingTemplate} which must not be
	 * null.
	 * @param messagingTemplate the messaging template to use for reading messages
	 * @param targetType the target type of items to convert messages to
	 */
	public MessageChannelItemReader(MessagingTemplate messagingTemplate, Class<T> targetType) {
		this.targetType = targetType;
		this.messagingTemplate = messagingTemplate;
		MessageChannel defaultDestination = messagingTemplate.getDefaultDestination();
		Assert.notNull(defaultDestination, "MessagingTemplate must have a default destination configured");
		this.messageChannel = defaultDestination;
	}

	/**
	 * Create a new {@link MessageChannelItemReader} instance. Messages will be read from
	 * the provided target message channel.
	 * @param messagingTemplate the messaging template to use for receiving messages
	 * @param messageChannel the message channel to read messages from
	 * @param targetType the target type of items to convert messages to
	 */
	public MessageChannelItemReader(MessagingTemplate messagingTemplate, MessageChannel messageChannel,
			Class<T> targetType) {
		this.messagingTemplate = messagingTemplate;
		this.messageChannel = messageChannel;
		this.targetType = targetType;
	}

	/**
	 * Set the source message channel.
	 * @param messageChannel the message channel to read messages from
	 */
	public void setMessageChannel(MessageChannel messageChannel) {
		this.messageChannel = messageChannel;
	}

	@Override
	public @Nullable T read() throws Exception {
		return this.messagingTemplate.receiveAndConvert(this.messageChannel, this.targetType);
	}

}
