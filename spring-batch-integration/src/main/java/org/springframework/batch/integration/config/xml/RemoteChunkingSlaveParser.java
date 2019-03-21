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

import org.w3c.dom.Element;

import org.springframework.batch.core.step.item.SimpleChunkProcessor;
import org.springframework.batch.integration.chunk.ChunkProcessorChunkHandler;
import org.springframework.batch.item.support.PassThroughItemProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ServiceActivatorFactoryBean;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * <p>
 * Parser for the remote-chunking-slave namespace element. If an
 * {@link org.springframework.batch.item.ItemProcessor} is not provided, an
 * {@link org.springframework.batch.item.support.PassThroughItemProcessor} will be
 * configured.
 * </p>
 *
 * @author Chris Schaefer
 * @since 3.1
 */
public class RemoteChunkingSlaveParser extends AbstractBeanDefinitionParser {
	private static final String INPUT_CHANNEL_ATTRIBUTE = "input-channel";
	private static final String OUTPUT_CHANNEL_ATTRIBUTE = "output-channel";
	private static final String ITEM_PROCESSOR_ATTRIBUTE = "item-processor";
	private static final String ITEM_WRITER_ATTRIBUTE = "item-writer";
	private static final String ITEM_PROCESSOR_PROPERTY_NAME = "itemProcessor";
	private static final String ITEM_WRITER_PROPERTY_NAME = "itemWriter";
	private static final String CHUNK_PROCESSOR_PROPERTY_NAME = "chunkProcessor";
	private static final String CHUNK_PROCESSOR_CHUNK_HANDLER_BEAN_NAME_PREFIX = "chunkProcessorChunkHandler_";

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		String id = element.getAttribute(ID_ATTRIBUTE);
		Assert.hasText(id, "The id attribute must be specified");

		String inputChannel = element.getAttribute(INPUT_CHANNEL_ATTRIBUTE);
		Assert.hasText(inputChannel, "The input-channel attribute must be specified");

		String outputChannel = element.getAttribute(OUTPUT_CHANNEL_ATTRIBUTE);
		Assert.hasText(outputChannel, "The output-channel attribute must be specified");

		String itemProcessor = element.getAttribute(ITEM_PROCESSOR_ATTRIBUTE);

		String itemWriter = element.getAttribute(ITEM_WRITER_ATTRIBUTE);
		Assert.hasText(itemWriter, "The item-writer attribute must be specified");

		BeanDefinitionRegistry beanDefinitionRegistry = parserContext.getRegistry();

		BeanDefinitionBuilder chunkProcessorBuilder =
				BeanDefinitionBuilder
						.genericBeanDefinition(SimpleChunkProcessor.class)
						.addPropertyReference(ITEM_WRITER_PROPERTY_NAME, itemWriter);

		if(StringUtils.hasText(itemProcessor)) {
			chunkProcessorBuilder.addPropertyReference(ITEM_PROCESSOR_PROPERTY_NAME, itemProcessor);
		} else {
			chunkProcessorBuilder.addPropertyValue(ITEM_PROCESSOR_PROPERTY_NAME, new PassThroughItemProcessor<>());
		}

		BeanDefinition chunkProcessorChunkHandler =
				BeanDefinitionBuilder
						.genericBeanDefinition(ChunkProcessorChunkHandler.class)
						.addPropertyValue(CHUNK_PROCESSOR_PROPERTY_NAME, chunkProcessorBuilder.getBeanDefinition())
						.getBeanDefinition();

		beanDefinitionRegistry.registerBeanDefinition(CHUNK_PROCESSOR_CHUNK_HANDLER_BEAN_NAME_PREFIX + id, chunkProcessorChunkHandler);

		new ServiceActivatorParser(id).parse(element, parserContext);

		return null;
	}

	private static class ServiceActivatorParser extends AbstractConsumerEndpointParser {
		private static final String TARGET_METHOD_NAME_PROPERTY_NAME = "targetMethodName";
		private static final String TARGET_OBJECT_PROPERTY_NAME = "targetObject";
		private static final String HANDLE_CHUNK_METHOD_NAME = "handleChunk";
		private static final String CHUNK_PROCESSOR_CHUNK_HANDLER_BEAN_NAME_PREFIX = "chunkProcessorChunkHandler_";

		private String id;

		public ServiceActivatorParser(String id) {
			this.id = id;
		}

		@Override
		protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ServiceActivatorFactoryBean.class);
			builder.addPropertyValue(TARGET_METHOD_NAME_PROPERTY_NAME, HANDLE_CHUNK_METHOD_NAME);
			builder.addPropertyValue(TARGET_OBJECT_PROPERTY_NAME, new RuntimeBeanReference(CHUNK_PROCESSOR_CHUNK_HANDLER_BEAN_NAME_PREFIX + id));
			return builder;
		}
	}
}
