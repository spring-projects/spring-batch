/*
 * Copyright 2014-2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.step.item.ChunkProcessor;
import org.springframework.batch.core.step.item.SimpleChunkProcessor;
import org.springframework.batch.integration.chunk.ChunkRequestHandler;
import org.springframework.batch.integration.chunk.ChunkMessageChannelItemWriter;
import org.springframework.batch.integration.chunk.ChunkProcessorChunkRequestHandler;
import org.springframework.batch.integration.chunk.RemoteChunkHandlerFactoryBean;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.PassThroughItemProcessor;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.config.ServiceActivatorFactoryBean;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>
 * Test cases for the {@link RemoteChunkingWorkerParser} and
 * {@link RemoteChunkingManagerParser}.
 * </p>
 *
 * @author Chris Schaefer
 * @author Mahmoud Ben Hassine
 * @since 3.1
 */
@SuppressWarnings("unchecked")
class RemoteChunkingParserTests {

	@SuppressWarnings("rawtypes")
	@Test
	void testRemoteChunkingWorkerParserWithProcessorDefined() {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"/org/springframework/batch/integration/config/xml/RemoteChunkingWorkerParserTests.xml");

		ChunkRequestHandler chunkRequestHandler = applicationContext.getBean(ChunkProcessorChunkRequestHandler.class);
		ChunkProcessor chunkProcessor = (SimpleChunkProcessor) TestUtils.getPropertyValue(chunkRequestHandler,
				"chunkProcessor");
		assertNotNull(chunkProcessor, "ChunkProcessor must not be null");

		ItemWriter<String> itemWriter = (ItemWriter<String>) TestUtils.getPropertyValue(chunkProcessor, "itemWriter");
		assertNotNull(itemWriter, "ChunkProcessor ItemWriter must not be null");
		assertTrue(itemWriter instanceof Writer, "Got wrong instance of ItemWriter");

		ItemProcessor<String, String> itemProcessor = (ItemProcessor<String, String>) TestUtils
			.getPropertyValue(chunkProcessor, "itemProcessor");
		assertNotNull(itemProcessor, "ChunkProcessor ItemWriter must not be null");
		assertTrue(itemProcessor instanceof Processor, "Got wrong instance of ItemProcessor");

		FactoryBean serviceActivatorFactoryBean = applicationContext.getBean(ServiceActivatorFactoryBean.class);
		assertNotNull(serviceActivatorFactoryBean, "ServiceActivatorFactoryBean must not be null");
		assertNotNull(TestUtils.getPropertyValue(serviceActivatorFactoryBean, "outputChannelName"),
				"Output channel name must not be null");

		MessageChannel inputChannel = applicationContext.getBean("requests", MessageChannel.class);
		assertNotNull(inputChannel, "Input channel must not be null");

		String targetMethodName = (String) TestUtils.getPropertyValue(serviceActivatorFactoryBean, "targetMethodName");
		assertNotNull(targetMethodName, "Target method name must not be null");
		assertEquals("handle", targetMethodName, "Target method name must be handle, got: " + targetMethodName);

		ChunkRequestHandler targetObject = (ChunkRequestHandler) TestUtils.getPropertyValue(serviceActivatorFactoryBean,
				"targetObject");
		assertNotNull(targetObject, "Target object must not be null");
	}

	@SuppressWarnings("rawtypes")
	@Test
	void testRemoteChunkingWorkerParserWithProcessorNotDefined() {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"/org/springframework/batch/integration/config/xml/RemoteChunkingWorkerParserNoProcessorTests.xml");

		ChunkRequestHandler chunkRequestHandler = applicationContext.getBean(ChunkProcessorChunkRequestHandler.class);
		ChunkProcessor chunkProcessor = (SimpleChunkProcessor) TestUtils.getPropertyValue(chunkRequestHandler,
				"chunkProcessor");
		assertNotNull(chunkProcessor, "ChunkProcessor must not be null");

		ItemProcessor<String, String> itemProcessor = (ItemProcessor<String, String>) TestUtils
			.getPropertyValue(chunkProcessor, "itemProcessor");
		assertNotNull(itemProcessor, "ChunkProcessor ItemWriter must not be null");
		assertTrue(itemProcessor instanceof PassThroughItemProcessor, "Got wrong instance of ItemProcessor");
	}

	@SuppressWarnings("rawtypes")
	@Test
	void testRemoteChunkingManagerParser() {
		ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
				"/org/springframework/batch/integration/config/xml/RemoteChunkingManagerParserTests.xml");

		ItemWriter itemWriter = applicationContext.getBean("itemWriter", ChunkMessageChannelItemWriter.class);
		assertNotNull(TestUtils.getPropertyValue(itemWriter, "messagingGateway"),
				"Messaging template must not be null");
		assertNotNull(TestUtils.getPropertyValue(itemWriter, "replyChannel"), "Reply channel must not be null");

		FactoryBean<ChunkRequestHandler> remoteChunkingHandlerFactoryBean = applicationContext
			.getBean(RemoteChunkHandlerFactoryBean.class);
		assertNotNull(TestUtils.getPropertyValue(remoteChunkingHandlerFactoryBean, "chunkWriter"),
				"Chunk writer must not be null");
		assertNotNull(TestUtils.getPropertyValue(remoteChunkingHandlerFactoryBean, "step"), "Step must not be null");
	}

	@Test
	void testRemoteChunkingManagerIdAttrAssert() {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
		applicationContext.setValidating(false);
		applicationContext.setConfigLocation(
				"/org/springframework/batch/integration/config/xml/RemoteChunkingManagerParserMissingIdAttrTests.xml");

		Exception exception = assertThrows(BeanDefinitionStoreException.class, applicationContext::refresh);
		assertTrue(exception.getCause() instanceof IllegalArgumentException,
				"Nested exception must be of type IllegalArgumentException");

		IllegalArgumentException iae = (IllegalArgumentException) exception.getCause();
		assertEquals("The id attribute must be specified", iae.getMessage());
	}

	@Test
	void testRemoteChunkingManagerMessageTemplateAttrAssert() {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
		applicationContext.setValidating(false);
		applicationContext.setConfigLocation(
				"/org/springframework/batch/integration/config/xml/RemoteChunkingManagerParserMissingMessageTemplateAttrTests.xml");

		Exception exception = assertThrows(BeanDefinitionStoreException.class, applicationContext::refresh);
		assertTrue(exception.getCause() instanceof IllegalArgumentException,
				"Nested exception must be of type IllegalArgumentException");

		IllegalArgumentException iae = (IllegalArgumentException) exception.getCause();
		assertEquals("The message-template attribute must be specified", iae.getMessage());
	}

	@Test
	void testRemoteChunkingManagerStepAttrAssert() {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
		applicationContext.setValidating(false);
		applicationContext.setConfigLocation(
				"/org/springframework/batch/integration/config/xml/RemoteChunkingManagerParserMissingStepAttrTests.xml");

		Exception exception = assertThrows(BeanDefinitionStoreException.class, applicationContext::refresh);
		assertTrue(exception.getCause() instanceof IllegalArgumentException,
				"Nested exception must be of type IllegalArgumentException");

		IllegalArgumentException iae = (IllegalArgumentException) exception.getCause();
		assertEquals("The step attribute must be specified", iae.getMessage());
	}

	@Test
	void testRemoteChunkingManagerReplyChannelAttrAssert() {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
		applicationContext.setValidating(false);
		applicationContext.setConfigLocation(
				"/org/springframework/batch/integration/config/xml/RemoteChunkingManagerParserMissingReplyChannelAttrTests.xml");

		Exception exception = assertThrows(BeanDefinitionStoreException.class, applicationContext::refresh);
		assertTrue(exception.getCause() instanceof IllegalArgumentException,
				"Nested exception must be of type IllegalArgumentException");

		IllegalArgumentException iae = (IllegalArgumentException) exception.getCause();
		assertEquals("The reply-channel attribute must be specified", iae.getMessage());
	}

	@Test
	void testRemoteChunkingWorkerIdAttrAssert() {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
		applicationContext.setValidating(false);
		applicationContext.setConfigLocation(
				"/org/springframework/batch/integration/config/xml/RemoteChunkingWorkerParserMissingIdAttrTests.xml");

		Exception exception = assertThrows(BeanDefinitionStoreException.class, applicationContext::refresh);
		assertTrue(exception.getCause() instanceof IllegalArgumentException,
				"Nested exception must be of type IllegalArgumentException");

		IllegalArgumentException iae = (IllegalArgumentException) exception.getCause();
		assertEquals("The id attribute must be specified", iae.getMessage());
	}

	@Test
	void testRemoteChunkingWorkerInputChannelAttrAssert() {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
		applicationContext.setValidating(false);
		applicationContext.setConfigLocation(
				"/org/springframework/batch/integration/config/xml/RemoteChunkingWorkerParserMissingInputChannelAttrTests.xml");

		Exception exception = assertThrows(BeanDefinitionStoreException.class, applicationContext::refresh);
		assertTrue(exception.getCause() instanceof IllegalArgumentException,
				"Nested exception must be of type IllegalArgumentException");

		IllegalArgumentException iae = (IllegalArgumentException) exception.getCause();
		assertEquals("The input-channel attribute must be specified", iae.getMessage());
	}

	@Test
	void testRemoteChunkingWorkerItemWriterAttrAssert() {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
		applicationContext.setValidating(false);
		applicationContext.setConfigLocation(
				"/org/springframework/batch/integration/config/xml/RemoteChunkingWorkerParserMissingItemWriterAttrTests.xml");

		Exception exception = assertThrows(BeanDefinitionStoreException.class, applicationContext::refresh);
		assertTrue(exception.getCause() instanceof IllegalArgumentException,
				"Nested exception must be of type IllegalArgumentException");

		IllegalArgumentException iae = (IllegalArgumentException) exception.getCause();
		assertEquals("The item-writer attribute must be specified", iae.getMessage());
	}

	@Test
	void testRemoteChunkingWorkerOutputChannelAttrAssert() {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
		applicationContext.setValidating(false);
		applicationContext.setConfigLocation(
				"/org/springframework/batch/integration/config/xml/RemoteChunkingWorkerParserMissingOutputChannelAttrTests.xml");

		Exception exception = assertThrows(BeanDefinitionStoreException.class, applicationContext::refresh);
		assertTrue(exception.getCause() instanceof IllegalArgumentException,
				"Nested exception must be of type IllegalArgumentException");

		IllegalArgumentException iae = (IllegalArgumentException) exception.getCause();
		assertEquals("The output-channel attribute must be specified", iae.getMessage());
	}

	private static class Writer implements ItemWriter<String> {

		@Override
		public void write(Chunk<? extends String> items) throws Exception {
			//
		}

	}

	private static class Processor implements ItemProcessor<String, String> {

		@Override
		public @Nullable String process(String item) throws Exception {
			return item;
		}

	}

}
