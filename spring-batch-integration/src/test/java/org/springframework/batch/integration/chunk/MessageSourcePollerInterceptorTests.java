/*
 * Copyright 2010-2023 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.springframework.batch.integration.chunk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

class MessageSourcePollerInterceptorTests {

	@Test
	void testPreReceive() {
		MessageSourcePollerInterceptor interceptor = new MessageSourcePollerInterceptor(new TestMessageSource("foo"));
		QueueChannel channel = new QueueChannel();
		assertTrue(interceptor.preReceive(channel));
		assertEquals("foo", channel.receive(10L).getPayload());
	}

	private static class TestMessageSource implements MessageSource<String> {

		private final String payload;

		public TestMessageSource(String payload) {
			super();
			this.payload = payload;
		}

		@Override
		public Message<String> receive() {
			return new GenericMessage<>(payload);
		}

	}

}
