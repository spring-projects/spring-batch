/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.batch.integration.config.xml;

import org.springframework.batch.integration.chunk.ChunkMessageChannelItemWriter;
import org.springframework.batch.integration.chunk.RemoteChunkHandlerFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.w3c.dom.Element;

/**
 * <p>
 * Parser for the remote-chunking-master namespace element.
 * </p>
 *
 * @author Chris Schaefer
 * @since 3.1
 */
public class RemoteChunkingMasterParser extends AbstractBeanDefinitionParser {
	private static final String MESSAGE_TEMPLATE_ATTRIBUTE = "message-template";
	private static final String STEP_ATTRIBUTE = "step";
	private static final String REPLY_CHANNEL_ATTRIBUTE = "reply-channel";
	private static final String MESSAGING_OPERATIONS_PROPERTY = "messagingOperations";
	private static final String REPLY_CHANNEL_PROPERTY = "replyChannel";
	private static final String CHUNK_WRITER_PROPERTY = "chunkWriter";
	private static final String STEP_PROPERTY = "step";
	private static final String CHUNK_HANDLER_BEAN_NAME_PREFIX = "remoteChunkHandlerFactoryBean_";

	@Override
	public AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		String id = element.getAttribute(ID_ATTRIBUTE);
		Assert.hasText(id, "The id attribute must be specified");

		String messageTemplate = element.getAttribute(MESSAGE_TEMPLATE_ATTRIBUTE);
		Assert.hasText(messageTemplate, "The message-template attribute must be specified");

		String step = element.getAttribute(STEP_ATTRIBUTE);
		Assert.hasText(step, "The step attribute must be specified");

		String replyChannel = element.getAttribute(REPLY_CHANNEL_ATTRIBUTE);
		Assert.hasText(replyChannel, "The reply-channel attribute must be specified");

		BeanDefinitionRegistry beanDefinitionRegistry = parserContext.getRegistry();

		BeanDefinition chunkMessageChannelItemWriter =
				BeanDefinitionBuilder
						.genericBeanDefinition(ChunkMessageChannelItemWriter.class)
						.addPropertyReference(MESSAGING_OPERATIONS_PROPERTY, messageTemplate)
						.addPropertyReference(REPLY_CHANNEL_PROPERTY, replyChannel)
						.getBeanDefinition();

		beanDefinitionRegistry.registerBeanDefinition(id, chunkMessageChannelItemWriter);

		BeanDefinition remoteChunkHandlerFactoryBean =
				BeanDefinitionBuilder
						.genericBeanDefinition(RemoteChunkHandlerFactoryBean.class)
						.addPropertyValue(CHUNK_WRITER_PROPERTY, chunkMessageChannelItemWriter)
						.addPropertyValue(STEP_PROPERTY, step)
						.getBeanDefinition();

		beanDefinitionRegistry.registerBeanDefinition(CHUNK_HANDLER_BEAN_NAME_PREFIX + step, remoteChunkHandlerFactoryBean);

		return null;
	}
}
