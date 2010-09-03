package org.springframework.batch.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@MessageEndpoint
public class SmokeTests {

	@Autowired
	private MessageChannel smokein;

	@Autowired
	private PollableChannel smokeout;
	
	// This has to be static because Spring Integration registers the handler
	// more than once (every time a test instance is created), but only one of
	// them will get the message.
	private volatile static int count = 0;

	@ServiceActivator(inputChannel = "smokein", outputChannel = "smokeout")
	public String process(String message) {
		count++;
		String result = message + ": " + count;
		return result;
	}

	@Test
	public void testDummyWithSimpleAssert() throws Exception {
		assertTrue(true);
	}

	@Test
	public void testVanillaSendAndReceive() throws Exception {
		smokein.send(new GenericMessage<String>("foo"));
		@SuppressWarnings("unchecked")
		Message<String> message = (Message<String>) smokeout.receive(100);
		String result = (String) (message == null ? null : message.getPayload());
		assertEquals("foo: 1", result);
		assertEquals(1, count);
	}

}
