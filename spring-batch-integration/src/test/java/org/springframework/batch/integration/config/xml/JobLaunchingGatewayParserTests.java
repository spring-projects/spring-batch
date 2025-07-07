/*
 * Copyright 2002-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.batch.integration.config.xml;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.integration.launch.JobLaunchingMessageHandler;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.test.util.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gunnar Hillert
 * @author Mahmoud Ben Hassine
 * @since 1.3
 *
 */
class JobLaunchingGatewayParserTests {

	private ConfigurableApplicationContext context;

	private EventDrivenConsumer consumer;

	@Test
	void testGatewayParser() {
		setUp("JobLaunchingGatewayParserTests-context.xml", getClass());

		final AbstractMessageChannel inputChannel = TestUtils.getPropertyValue(this.consumer, "inputChannel",
				AbstractMessageChannel.class);
		assertEquals("requestChannel", inputChannel.getComponentName());

		final JobLaunchingMessageHandler jobLaunchingMessageHandler = TestUtils.getPropertyValue(this.consumer,
				"handler.jobLaunchingMessageHandler", JobLaunchingMessageHandler.class);

		assertNotNull(jobLaunchingMessageHandler);

		final MessagingTemplate messagingTemplate = TestUtils.getPropertyValue(this.consumer,
				"handler.messagingTemplate", MessagingTemplate.class);
		final Long sendTimeout = TestUtils.getPropertyValue(messagingTemplate, "sendTimeout", Long.class);

		assertEquals(123L, sendTimeout, "Wrong sendTimeout");
		assertFalse(this.consumer.isRunning());
	}

	@Test
	void testJobLaunchingGatewayIsRunning() {
		setUp("JobLaunchingGatewayParserTestsRunning-context.xml", getClass());
		assertTrue(this.consumer.isRunning());

		final MessagingTemplate messagingTemplate = TestUtils.getPropertyValue(this.consumer,
				"handler.messagingTemplate", MessagingTemplate.class);
		final Long sendTimeout = TestUtils.getPropertyValue(messagingTemplate, "sendTimeout", Long.class);

		assertEquals(30000, sendTimeout, "Wrong sendTimeout");
	}

	@Test
	void testJobLaunchingGatewayNoJobLauncher() {
		Exception exception = assertThrows(BeanCreationException.class,
				() -> setUp("JobLaunchingGatewayParserTestsNoJobLauncher-context.xml", getClass()));
		assertEquals("No bean named 'jobLauncher' available", exception.getCause().getMessage());
	}

	@Test
	@Disabled("Seems like EnableBatchProcessing is not being picked up in this test")
	void testJobLaunchingGatewayWithEnableBatchProcessing() {

		setUp("JobLaunchingGatewayParserTestsWithEnableBatchProcessing-context.xml", getClass());
		final JobLaunchingMessageHandler jobLaunchingMessageHandler = TestUtils.getPropertyValue(this.consumer,
				"handler.jobLaunchingMessageHandler", JobLaunchingMessageHandler.class);
		assertNotNull(jobLaunchingMessageHandler);

		final JobLauncher jobLauncher = TestUtils.getPropertyValue(jobLaunchingMessageHandler, "jobLauncher",
				JobLauncher.class);
		assertNotNull(jobLauncher);

	}

	@AfterEach
	void tearDown() {
		if (context != null) {
			context.close();
		}
	}

	private void setUp(String name, Class<?> cls) {
		context = new ClassPathXmlApplicationContext(name, cls);
		consumer = this.context.getBean("batchjobExecutor", EventDrivenConsumer.class);
	}

}
