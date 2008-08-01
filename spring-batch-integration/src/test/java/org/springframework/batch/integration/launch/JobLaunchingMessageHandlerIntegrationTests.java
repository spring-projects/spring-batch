package org.springframework.batch.integration.launch;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.integration.JobSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class JobLaunchingMessageHandlerIntegrationTests {

	@Autowired
	@Qualifier("requests")
	private PollableChannel requestChannel;

	@Autowired
	@Qualifier("response")
	private PollableChannel responseChannel;

	private JobSupport job = new JobSupport("testJob");

	@Before
	public void setUp() {
		requestChannel.purge(null);
		responseChannel.purge(null);
	}

	@Test
	@DirtiesContext
	@SuppressWarnings("unchecked")
	public void testNoReply() {
		GenericMessage<JobLaunchRequest> trigger = new GenericMessage<JobLaunchRequest>(new JobLaunchRequest(job,
				new JobParameters()));
		try {
			requestChannel.send(trigger);
		}
		catch (MessageHandlingException e) {
			String message = e.getMessage();
			assertTrue("Wrong message: " + message, message.contains("reply channel"));
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
		MessageHeaders headers = new MessageHeaders(new HashMap<String, Object>());
		headers.put(MessageHeaders.RETURN_ADDRESS, "response");
		GenericMessage<JobLaunchRequest> trigger = new GenericMessage<JobLaunchRequest>(new JobLaunchRequest(job,
				builder.toJobParameters()), headers);
		requestChannel.send(trigger);
		Message<JobExecution> executionMessage = (Message<JobExecution>) responseChannel.receive(1000);

		assertNotNull("No response received", executionMessage);
		JobExecution execution = executionMessage.getPayload();
		assertNotNull("JobExectuion not returned", execution);
	}

}
