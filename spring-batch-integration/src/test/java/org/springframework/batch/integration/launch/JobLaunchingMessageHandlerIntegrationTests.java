package org.springframework.batch.integration.launch;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class JobLaunchingMessageHandlerIntegrationTests {

	@Autowired
	@Qualifier("requests")
	private MessageChannel requestChannel;

	@Autowired
	@Qualifier("response")
	private PollableChannel responseChannel;

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

}
