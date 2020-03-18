/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.batch.item.jms.builder;

import java.util.Date;

import javax.jms.Message;

import org.junit.Before;
import org.junit.Test;

import org.springframework.batch.item.jms.JmsItemReader;
import org.springframework.jms.core.JmsOperations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 */
public class JmsItemReaderBuilderTests {

	private JmsOperations defaultJmsTemplate;

	@Before
	public void setupJmsTemplate() {
		this.defaultJmsTemplate = mock(JmsOperations.class);
		when(this.defaultJmsTemplate.receiveAndConvert()).thenReturn("foo");
	}

	@Test
	public void testBasicRead() {
		JmsItemReader<String> itemReader = new JmsItemReaderBuilder<String>().jmsTemplate(this.defaultJmsTemplate)
				.build();
		assertEquals("foo", itemReader.read());
	}

	@Test
	public void testSetItemSubclassType() {
		JmsOperations jmsTemplate = mock(JmsOperations.class);

		Date date = new java.sql.Date(0L);
		when(jmsTemplate.receiveAndConvert()).thenReturn(date);

		JmsItemReader<Date> itemReader = new JmsItemReaderBuilder<Date>().jmsTemplate(jmsTemplate).itemType(Date.class)
				.build();
		assertEquals(date, itemReader.read());
	}

	@Test
	public void testSetItemTypeMismatch() {
		JmsItemReader<Date> itemReader = new JmsItemReaderBuilder<Date>().jmsTemplate(this.defaultJmsTemplate)
				.itemType(Date.class).build();
		try {
			itemReader.read();
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
			assertTrue(e.getMessage().indexOf("wrong type") >= 0);
		}
	}

	@Test
	public void testMessageType() {
		JmsOperations jmsTemplate = mock(JmsOperations.class);
		Message message = mock(Message.class);
		when(jmsTemplate.receive()).thenReturn(message);

		JmsItemReader<Message> itemReader = new JmsItemReaderBuilder<Message>().jmsTemplate(jmsTemplate)
				.itemType(Message.class).build();
		assertEquals(message, itemReader.read());
	}

	@Test
	public void testNullJmsTemplate() {
		try {
			new JmsItemReaderBuilder<String>().itemType(String.class).build();
			fail("IllegalArgumentException should have been thrown");
		}
		catch (IllegalArgumentException ise) {
			assertEquals("IllegalArgumentException message did not match the expected result.",
					"jmsTemplate is required.", ise.getMessage());
		}
	}
}
