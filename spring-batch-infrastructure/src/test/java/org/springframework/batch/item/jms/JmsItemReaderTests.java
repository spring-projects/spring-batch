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
import javax.jms.Queue;

import org.easymock.EasyMock;
import org.junit.Test;
import org.springframework.jms.core.JmsOperations;

public class JmsItemReaderTests {

	JmsItemReader<Object> itemProvider = new JmsItemReader<Object>();

	@Test
	public void testNoItemTypeSunnyDay() {
		JmsOperations jmsTemplate = EasyMock.createMock(JmsOperations.class);
		EasyMock.expect(jmsTemplate.receiveAndConvert()).andReturn("foo");
		EasyMock.replay(jmsTemplate);

		itemProvider.setJmsTemplate(jmsTemplate);
		assertEquals("foo", itemProvider.read());
		EasyMock.verify(jmsTemplate);
	}

	@Test
	public void testSetItemTypeSunnyDay() {
		JmsOperations jmsTemplate = EasyMock.createMock(JmsOperations.class);
		EasyMock.expect(jmsTemplate.receiveAndConvert()).andReturn("foo");
		EasyMock.replay(jmsTemplate);

		itemProvider.setJmsTemplate(jmsTemplate);
		itemProvider.setItemType(String.class);
		assertEquals("foo", itemProvider.read());
		EasyMock.verify(jmsTemplate);
	}

	@Test
	public void testSetItemSubclassTypeSunnyDay() {
		JmsOperations jmsTemplate = EasyMock.createMock(JmsOperations.class);

		Date date = new java.sql.Date(0L);
		EasyMock.expect(jmsTemplate.receiveAndConvert()).andReturn(date);
		EasyMock.replay(jmsTemplate);

		itemProvider.setJmsTemplate(jmsTemplate);
		itemProvider.setItemType(Date.class);
		assertEquals(date, itemProvider.read());
		EasyMock.verify(jmsTemplate);
	}

	@Test
	public void testSetItemTypeMismatch() {
		JmsOperations jmsTemplate = EasyMock.createMock(JmsOperations.class);
		EasyMock.expect(jmsTemplate.receiveAndConvert()).andReturn("foo");
		EasyMock.replay(jmsTemplate);

		itemProvider.setJmsTemplate(jmsTemplate);
		itemProvider.setItemType(Date.class);
		try {
			itemProvider.read();
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

		itemProvider.setJmsTemplate(jmsTemplate);
		itemProvider.setItemType(Message.class);
		assertEquals(message, itemProvider.read());
		EasyMock.verify(jmsTemplate);
	}

	@Test
	public void testRecoverWithNoDestination() throws Exception {
		JmsOperations jmsTemplate = EasyMock.createMock(JmsOperations.class);
		EasyMock.replay(jmsTemplate);

		itemProvider.setJmsTemplate(jmsTemplate);
		itemProvider.setItemType(String.class);
		itemProvider.recover("foo", null);

		EasyMock.verify(jmsTemplate);
	}

	@Test
	public void testErrorQueueWithDestinationName() throws Exception {
		JmsOperations jmsTemplate = EasyMock.createMock(JmsOperations.class);
		jmsTemplate.convertAndSend("queue", "foo");
		EasyMock.expectLastCall();
		EasyMock.replay(jmsTemplate);

		itemProvider.setJmsTemplate(jmsTemplate);
		itemProvider.setItemType(String.class);
		itemProvider.setErrorDestinationName("queue");
		itemProvider.recover("foo", null);

		EasyMock.verify(jmsTemplate);
	}

	@Test
	public void testErrorQueueWithDestination() throws Exception {
		JmsOperations jmsTemplate = EasyMock.createMock(JmsOperations.class);
		Queue queue = EasyMock.createMock(Queue.class);
		jmsTemplate.convertAndSend(queue, "foo");
		EasyMock.expectLastCall();
		EasyMock.replay(jmsTemplate, queue);

		itemProvider.setJmsTemplate(jmsTemplate);
		itemProvider.setItemType(String.class);
		itemProvider.setErrorDestination(queue);
		itemProvider.recover("foo", null);

		EasyMock.verify(jmsTemplate, queue);
	}

	@Test
	public void testGetKeyFromMessage() throws Exception {
		Message message = EasyMock.createMock(Message.class);
		EasyMock.expect(message.getJMSMessageID()).andReturn("foo");
		EasyMock.replay(message);

		itemProvider.setItemType(Message.class);
		assertEquals("foo", itemProvider.getKey(message));

		EasyMock.verify(message);
	}

	@Test
	public void testGetKeyFromNonMessage() throws Exception {
		itemProvider.setItemType(String.class);
		assertEquals("foo", itemProvider.getKey("foo"));
	}

	@Test
	public void testIsNewForMessage() throws Exception {
		Message message = EasyMock.createMock(Message.class);
		EasyMock.expect(message.getJMSRedelivered()).andReturn(true);
		EasyMock.replay(message);

		itemProvider.setItemType(Message.class);
		assertEquals(false, itemProvider.isNew(message));
		
		EasyMock.verify(message);
	}

	@Test
	public void testIsNewForNonMessage() throws Exception {
		itemProvider.setItemType(String.class);
		assertEquals(false, itemProvider.isNew("foo"));
	}
}
