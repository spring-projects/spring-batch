/*
 * Copyright 2006-2022 the original author or authors.
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
package org.springframework.batch.item.jms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.jms.Message;

import org.junit.jupiter.api.Test;

import org.springframework.jms.core.JmsTemplate;

/**
 * @author Dave Syer
 * @author Will Schipp
 * @author Mahmoud Ben Hassine
 *
 */
class JmsMethodArgumentsKeyGeneratorTests {

	private final JmsMethodArgumentsKeyGenerator methodArgumentsKeyGenerator = new JmsMethodArgumentsKeyGenerator();

	@Test
	void testGetKeyFromMessage() throws Exception {
		Message message = mock();
		JmsTemplate jmsTemplate = mock();
		when(message.getJMSMessageID()).thenReturn("foo");
		when(jmsTemplate.getReceiveTimeout()).thenReturn(1000L);
		when(jmsTemplate.getDefaultDestinationName()).thenReturn("destination");

		JmsItemReader<Message> itemReader = new JmsItemReader<>(jmsTemplate);
		itemReader.setItemType(Message.class);
		assertEquals("foo", methodArgumentsKeyGenerator.getKey(new Object[] { message }));

	}

	@Test
	void testGetKeyFromNonMessage() {
		assertEquals("foo", methodArgumentsKeyGenerator.getKey(new Object[] { "foo" }));
	}

}
