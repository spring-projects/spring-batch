/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.batch.integration.launch;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.integration.JobSupport;
import org.springframework.batch.integration.step.TestTasklet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 * @author Gunnar Hillert
 * @since 1.3
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class JobLaunchingGatewayIntegrationTests {

	@Autowired
	@Qualifier("requests")
	private MessageChannel requestChannel;

	@Autowired
	@Qualifier("response")
	private PollableChannel responseChannel;

	@Autowired
	private TestTasklet tasklet;

	@Autowired
	private Job testJob;

	private final JobSupport job = new JobSupport("testJob");

	@Before
	public void setUp() {
		Object message = "";
		while (message!=null) {
			message = responseChannel.receive(10L);
		}
	}

	@Test
	@DirtiesContext
	@SuppressWarnings("unchecked")
	public void testNoReply() {
		GenericMessage<JobLaunchRequest> trigger = new GenericMessage<>(new JobLaunchRequest(job,
				new JobParameters()));
		try {
			requestChannel.send(trigger);
			fail();
		}
		catch (MessagingException e) {
			String message = e.getMessage();
			assertTrue("Wrong message: " + message, message.contains("replyChannel"));
		}
		Message<JobExecution> executionMessage = (Message<JobExecution>) responseChannel.receive(1000);

		assertNull("JobExecution message received when no return address set", executionMessage);
	}

	@SuppressWarnings("unchecked")
	@Test
	@DirtiesContext
	public void testReply() {
		JobParametersBuilder builder = new JobParametersBuilder();
		builder.addString("dontclash", "12");
		Map<String, Object> map = new HashMap<>();
		map.put(MessageHeaders.REPLY_CHANNEL, "response");
		MessageHeaders headers = new MessageHeaders(map);
		GenericMessage<JobLaunchRequest> trigger = new GenericMessage<>(new JobLaunchRequest(job,
				builder.toJobParameters()), headers);
		requestChannel.send(trigger);
		Message<JobExecution> executionMessage = (Message<JobExecution>) responseChannel.receive(1000);

		assertNotNull("No response received", executionMessage);
		JobExecution execution = executionMessage.getPayload();
		assertNotNull("JobExecution not returned", execution);
	}

	@Test
	@DirtiesContext
	@SuppressWarnings("unchecked")
	public void testWrongPayload() {

		final Message<String> stringMessage = MessageBuilder.withPayload("just text").build();

		try {
			requestChannel.send(stringMessage);
			fail();
		}
		catch (MessageHandlingException e) {
			String message = e.getCause().getMessage();
			assertTrue("Wrong message: " + message, message.contains("The payload must be of type JobLaunchRequest."));
		}
		Message<JobExecution> executionMessage = (Message<JobExecution>) responseChannel.receive(1000);

		assertNull("JobExecution message received when no return address set", executionMessage);
	}

	@Test
	@DirtiesContext
	@SuppressWarnings("unchecked")
	public void testExceptionRaised() throws InterruptedException {

		this.tasklet.setFail(true);

		JobParametersBuilder builder = new JobParametersBuilder();
		builder.addString("dontclash", "12");
		Map<String, Object> map = new HashMap<>();
		map.put(MessageHeaders.REPLY_CHANNEL, "response");
		MessageHeaders headers = new MessageHeaders(map);
		GenericMessage<JobLaunchRequest> trigger = new GenericMessage<>(new JobLaunchRequest(testJob,
				builder.toJobParameters()), headers);
		requestChannel.send(trigger);

		Message<JobExecution> executionMessage = (Message<JobExecution>) responseChannel.receive(1000);

		assertNotNull("No response received", executionMessage);

		JobExecution execution = executionMessage.getPayload();
		assertNotNull("JobExecution not returned", execution);

		assertEquals(ExitStatus.FAILED.getExitCode(), execution.getExitStatus().getExitCode());

	}
}
