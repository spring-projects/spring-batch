/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.item.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;

import javax.jms.Message;

import org.easymock.EasyMock;
import org.junit.Test;
import org.springframework.jms.core.JmsOperations;
import org.springframework.jms.core.JmsTemplate;

public class JmsItemReaderTests {

	JmsItemReader<String> itemReader = new JmsItemReader<String>();

	@Test
	public void testNoItemTypeSunnyDay() {
		JmsOperations jmsTemplate = EasyMock.createMock(JmsOperations.class);
		EasyMock.expect(jmsTemplate.receiveAndConvert()).andReturn("foo");
		EasyMock.replay(jmsTemplate);

		itemReader.setJmsTemplate(jmsTemplate);
		assertEquals("foo", itemReader.read());
		EasyMock.verify(jmsTemplate);
	}

	@Test
	public void testSetItemTypeSunnyDay() {
		JmsOperations jmsTemplate = EasyMock.createMock(JmsOperations.class);
		EasyMock.expect(jmsTemplate.receiveAndConvert()).andReturn("foo");
		EasyMock.replay(jmsTemplate);

		itemReader.setJmsTemplate(jmsTemplate);
		itemReader.setItemType(String.class);
		assertEquals("foo", itemReader.read());
		EasyMock.verify(jmsTemplate);
	}

	@Test
	public void testSetItemSubclassTypeSunnyDay() {
		JmsOperations jmsTemplate = EasyMock.createMock(JmsOperations.class);

		Date date = new java.sql.Date(0L);
		EasyMock.expect(jmsTemplate.receiveAndConvert()).andReturn(date);
		EasyMock.replay(jmsTemplate);

		JmsItemReader<Date> itemReader = new JmsItemReader<Date>();
		itemReader.setJmsTemplate(jmsTemplate);
		itemReader.setItemType(Date.class);
		assertEquals(date, itemReader.read());

		EasyMock.verify(jmsTemplate);
	}

	@Test
	public void testSetItemTypeMismatch() {
		JmsOperations jmsTemplate = EasyMock.createMock(JmsOperations.class);
		EasyMock.expect(jmsTemplate.receiveAndConvert()).andReturn("foo");
		EasyMock.replay(jmsTemplate);

		JmsItemReader<Date> itemReader = new JmsItemReader<Date>();
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
		EasyMock.verify(jmsTemplate);
	}

	@Test
	public void testNextMessageSunnyDay() {
		JmsOperations jmsTemplate = EasyMock.createMock(JmsOperations.class);
		Message message = EasyMock.createMock(Message.class);
		EasyMock.expect(jmsTemplate.receive()).andReturn(message);
		EasyMock.replay(jmsTemplate, message);

		JmsItemReader<Message> itemReader = new JmsItemReader<Message>();
		itemReader.setJmsTemplate(jmsTemplate);
		itemReader.setItemType(Message.class);
		assertEquals(message, itemReader.read());
		EasyMock.verify(jmsTemplate);
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
