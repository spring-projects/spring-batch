package org.springframework.integration.batch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.annotation.Handler;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration(locations = "/integration-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
@MessageEndpoint(input = "smokein", output = "smokeout")
public class SmokeTests {

	@Autowired
	@Qualifier("smokein")
	private MessageChannel smokein;

	@Autowired
	@Qualifier("smokeout")
	private MessageChannel smokeout;

	// This has to be static because the MessageBus registers the handler
	// more than once (every time a test instance is created), but only one of
	// them will get the message.
	private volatile static int count = 0;

	@Handler
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
		Message<?> message = smokeout.receive(100);
		String result = (String) (message == null ? null : message.getPayload());
		assertEquals("foo: 1", result);
		assertEquals(1, count);
	}

}
