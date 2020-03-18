/*
 * Copyright 2002-2013 the original author or authors.
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

import org.junit.After;
import org.junit.Test;

import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.integration.launch.JobLaunchingMessageHandler;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.test.util.TestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 * @author Gunnar Hillert
 * @since 1.3
 *
 */
public class JobLaunchingGatewayParserTests {

	private ConfigurableApplicationContext context;

	private EventDrivenConsumer consumer;

	@Test
	public void testGatewayParser() throws Exception {
		setUp("JobLaunchingGatewayParserTests-context.xml", getClass());

		final AbstractMessageChannel inputChannel = TestUtils.getPropertyValue(this.consumer, "inputChannel", AbstractMessageChannel.class);
		assertEquals("requestChannel", inputChannel.getComponentName());

		final JobLaunchingMessageHandler jobLaunchingMessageHandler = TestUtils.getPropertyValue(this.consumer, "handler.jobLaunchingMessageHandler", JobLaunchingMessageHandler.class);

		assertNotNull(jobLaunchingMessageHandler);

		final MessagingTemplate messagingTemplate = TestUtils.getPropertyValue(this.consumer, "handler.messagingTemplate", MessagingTemplate.class);
		final Long sendTimeout = TestUtils.getPropertyValue(messagingTemplate, "sendTimeout", Long.class);

		assertEquals("Wrong sendTimeout", Long.valueOf(123L),  sendTimeout);
		assertFalse(this.consumer.isRunning());
	}

	@Test
	public void testJobLaunchingGatewayIsRunning() throws Exception {
		setUp("JobLaunchingGatewayParserTestsRunning-context.xml", getClass());
		assertTrue(this.consumer.isRunning());

		final MessagingTemplate messagingTemplate = TestUtils.getPropertyValue(this.consumer, "handler.messagingTemplate", MessagingTemplate.class);
		final Long sendTimeout = TestUtils.getPropertyValue(messagingTemplate, "sendTimeout", Long.class);

		assertEquals("Wrong sendTimeout", Long.valueOf(-1L),  sendTimeout);
	}

	@Test
	public void testJobLaunchingGatewayNoJobLauncher() throws Exception {
		try {
			setUp("JobLaunchingGatewayParserTestsNoJobLauncher-context.xml", getClass());
		}
		catch(BeanCreationException e) {
			assertEquals("No bean named 'jobLauncher' available", e.getCause().getMessage());
			return;
		}
		fail("Expected a NoSuchBeanDefinitionException to be thrown.");
	}

	@Test
	public void testJobLaunchingGatewayWithEnableBatchProcessing() throws Exception {

		setUp("JobLaunchingGatewayParserTestsWithEnableBatchProcessing-context.xml", getClass());
		final JobLaunchingMessageHandler jobLaunchingMessageHandler = TestUtils.getPropertyValue(this.consumer, "handler.jobLaunchingMessageHandler", JobLaunchingMessageHandler.class);
		assertNotNull(jobLaunchingMessageHandler);

		final JobLauncher jobLauncher = TestUtils.getPropertyValue(jobLaunchingMessageHandler, "jobLauncher", JobLauncher.class);
		assertNotNull(jobLauncher);

	}

	@After
	public void tearDown(){
		if(context != null){
			context.close();
		}
	}

	public void setUp(String name, Class<?> cls){
		context    = new ClassPathXmlApplicationContext(name, cls);
		consumer   = this.context.getBean("batchjobExecutor", EventDrivenConsumer.class);
	}

}
