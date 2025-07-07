/*
 * Copyright 2008-2022 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.integration.JobSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
class JobLaunchingMessageHandlerIntegrationTests {

	@Autowired
	@Qualifier("requests")
	private MessageChannel requestChannel;

	@Autowired
	@Qualifier("response")
	private PollableChannel responseChannel;

	private final JobSupport job = new JobSupport("testJob");

	@BeforeEach
	void setUp() {
		Object message = "";
		while (message != null) {
			message = responseChannel.receive(10L);
		}
	}

	@Test
	@DirtiesContext
	@SuppressWarnings("unchecked")
	void testNoReply() {
		GenericMessage<JobLaunchRequest> trigger = new GenericMessage<>(new JobLaunchRequest(job, new JobParameters()));
		Exception exception = assertThrows(MessagingException.class, () -> requestChannel.send(trigger));
		String message = exception.getCause().getMessage();
		assertTrue(message.contains("replyChannel"), "Wrong message: " + message);

		Message<JobExecution> executionMessage = (Message<JobExecution>) responseChannel.receive(1000);
		assertNull(executionMessage, "JobExecution message received when no return address set");
	}

	@SuppressWarnings("unchecked")
	@Test
	@DirtiesContext
	void testReply() {
		JobParametersBuilder builder = new JobParametersBuilder();
		builder.addString("dontclash", "12");
		Map<String, Object> map = new HashMap<>();
		map.put(MessageHeaders.REPLY_CHANNEL, "response");
		MessageHeaders headers = new MessageHeaders(map);
		GenericMessage<JobLaunchRequest> trigger = new GenericMessage<>(
				new JobLaunchRequest(job, builder.toJobParameters()), headers);
		requestChannel.send(trigger);
		Message<JobExecution> executionMessage = (Message<JobExecution>) responseChannel.receive(1000);

		assertNotNull(executionMessage, "No response received");
		JobExecution execution = executionMessage.getPayload();
		assertNotNull(execution, "JobExecution not returned");
	}

}
