/*
 * Copyright 2006-2007 the original author or authors.
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;

import javax.jms.Message;

import org.junit.Test;
import org.springframework.jms.core.JmsOperations;
import org.springframework.jms.core.JmsTemplate;

public class JmsItemReaderTests {

	JmsItemReader<String> itemReader = new JmsItemReader<>();

	@Test
	public void testNoItemTypeSunnyDay() {
		JmsOperations jmsTemplate = mock(JmsOperations.class);
		when(jmsTemplate.receiveAndConvert()).thenReturn("foo");

		itemReader.setJmsTemplate(jmsTemplate);
		assertEquals("foo", itemReader.read());
	}

	@Test
	public void testSetItemTypeSunnyDay() {
		JmsOperations jmsTemplate = mock(JmsOperations.class);
		when(jmsTemplate.receiveAndConvert()).thenReturn("foo");

		itemReader.setJmsTemplate(jmsTemplate);
		itemReader.setItemType(String.class);
		assertEquals("foo", itemReader.read());
	}

	@Test
	public void testSetItemSubclassTypeSunnyDay() {
		JmsOperations jmsTemplate = mock(JmsOperations.class);

		Date date = new java.sql.Date(0L);
		when(jmsTemplate.receiveAndConvert()).thenReturn(date);

		JmsItemReader<Date> itemReader = new JmsItemReader<>();
		itemReader.setJmsTemplate(jmsTemplate);
		itemReader.setItemType(Date.class);
		assertEquals(date, itemReader.read());

	}

	@Test
	public void testSetItemTypeMismatch() {
		JmsOperations jmsTemplate = mock(JmsOperations.class);
		when(jmsTemplate.receiveAndConvert()).thenReturn("foo");

		JmsItemReader<Date> itemReader = new JmsItemReader<>();
		itemReader.setJmsTemplate(jmsTemplate);
		itemReader.setItemType(Date.class);
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
	public void testNextMessageSunnyDay() {
		JmsOperations jmsTemplate = mock(JmsOperations.class);
		Message message = mock(Message.class);
		when(jmsTemplate.receive()).thenReturn(message);

		JmsItemReader<Message> itemReader = new JmsItemReader<>();
		itemReader.setJmsTemplate(jmsTemplate);
		itemReader.setItemType(Message.class);
		assertEquals(message, itemReader.read());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testTemplateWithNoDefaultDestination() throws Exception {
		JmsTemplate jmsTemplate = new JmsTemplate();
		jmsTemplate.setReceiveTimeout(100L);
		itemReader.setJmsTemplate(jmsTemplate);		
	}

	@Test(expected=IllegalArgumentException.class)
	public void testTemplateWithNoTimeout() throws Exception {
		JmsTemplate jmsTemplate = new JmsTemplate();
		jmsTemplate.setDefaultDestinationName("foo");
		itemReader.setJmsTemplate(jmsTemplate);		
	}

}
