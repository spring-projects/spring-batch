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
package org.springframework.batch.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
class SmokeTests {

	@Autowired
	private MessageChannel smokein;

	@Autowired
	private PollableChannel smokeout;

	@Test
	void testDummyWithSimpleAssert() {
		assertTrue(true);
	}

	@Test
	void testVanillaSendAndReceive() {
		smokein.send(new GenericMessage<>("foo"));
		@SuppressWarnings("unchecked")
		Message<String> message = (Message<String>) smokeout.receive(100);
		String result = message == null ? null : message.getPayload();
		assertEquals("foo: 1", result);
		assertEquals(1, AnnotatedEndpoint.count);
	}

	@MessageEndpoint
	static class AnnotatedEndpoint {

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

	}

}
