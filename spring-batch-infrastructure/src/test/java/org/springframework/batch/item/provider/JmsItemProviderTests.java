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

package org.springframework.batch.item.provider;

import java.util.Date;

import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.Queue;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.jms.core.JmsOperations;

public class JmsItemProviderTests extends TestCase {

	JmsItemProvider itemProvider = new JmsItemProvider();

	public void testNoItemTypeSunnyDay() {
		MockControl templateControl = MockControl.createControl(JmsOperations.class);
		JmsOperations jmsTemplate = (JmsOperations) templateControl.getMock();
		templateControl.expectAndReturn(jmsTemplate.receiveAndConvert(), "foo");
		templateControl.replay();

		itemProvider.setJmsTemplate(jmsTemplate);
		assertEquals("foo", itemProvider.next());
		templateControl.verify();
	}

	public void testSetItemTypeSunnyDay() {
		MockControl templateControl = MockControl.createControl(JmsOperations.class);
		JmsOperations jmsTemplate = (JmsOperations) templateControl.getMock();
		templateControl.expectAndReturn(jmsTemplate.receiveAndConvert(), "foo");
		templateControl.replay();

		itemProvider.setJmsTemplate(jmsTemplate);
		itemProvider.setItemType(String.class);
		assertEquals("foo", itemProvider.next());
		templateControl.verify();
	}

	public void testSetItemSubclassTypeSunnyDay() {
		MockControl templateControl = MockControl.createControl(JmsOperations.class);
		JmsOperations jmsTemplate = (JmsOperations) templateControl.getMock();

		Date date = new java.sql.Date(0L);
		templateControl.expectAndReturn(jmsTemplate.receiveAndConvert(), date);
		templateControl.replay();

		itemProvider.setJmsTemplate(jmsTemplate);
		itemProvider.setItemType(Date.class);
		assertEquals(date, itemProvider.next());
		templateControl.verify();
	}

	public void testSetItemTypeMismatch() {
		MockControl templateControl = MockControl.createControl(JmsOperations.class);
		JmsOperations jmsTemplate = (JmsOperations) templateControl.getMock();
		templateControl.expectAndReturn(jmsTemplate.receiveAndConvert(), "foo");
		templateControl.replay();

		itemProvider.setJmsTemplate(jmsTemplate);
		itemProvider.setItemType(Date.class);
		try {
			itemProvider.next();
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
			assertTrue(e.getMessage().indexOf("wrong type") >= 0);
		}
		templateControl.verify();
	}

	public void testNextMessageSunnyDay() {
		MockControl templateControl = MockControl.createControl(JmsOperations.class);
		MockControl messageControl = MockControl.createControl(Message.class);
		JmsOperations jmsTemplate = (JmsOperations) templateControl.getMock();
		Message message = (Message) messageControl.getMock();
		templateControl.expectAndReturn(jmsTemplate.receive(), message);
		templateControl.replay();

		itemProvider.setJmsTemplate(jmsTemplate);
		itemProvider.setItemType(Message.class);
		assertEquals(message, itemProvider.next());
		templateControl.verify();
	}

	public void testRecoverWithNoDestination() throws Exception {
		MockControl templateControl = MockControl.createControl(JmsOperations.class);
		JmsOperations jmsTemplate = (JmsOperations) templateControl.getMock();
		templateControl.replay();

		itemProvider.setJmsTemplate(jmsTemplate);
		itemProvider.setItemType(String.class);
		itemProvider.recover("foo", null);

		templateControl.verify();
	}

	public void testErrorQueueWithDestinationName() throws Exception {
		MockControl templateControl = MockControl.createControl(JmsOperations.class);
		JmsOperations jmsTemplate = (JmsOperations) templateControl.getMock();
		jmsTemplate.convertAndSend("queue", "foo");
		templateControl.setVoidCallable();
		templateControl.replay();

		itemProvider.setJmsTemplate(jmsTemplate);
		itemProvider.setItemType(String.class);
		itemProvider.setErrorDestinationName("queue");
		itemProvider.recover("foo", null);
		templateControl.verify();
	}

	public void testErrorQueueWithDestination() throws Exception {
		MockControl templateControl = MockControl.createControl(JmsOperations.class);
		MockControl queueControl = MockControl.createControl(Queue.class);

		Destination queue = (Destination) queueControl.getMock();
		queueControl.replay();

		JmsOperations jmsTemplate = (JmsOperations) templateControl.getMock();
		jmsTemplate.convertAndSend(queue, "foo");
		templateControl.setVoidCallable();
		templateControl.replay();

		itemProvider.setJmsTemplate(jmsTemplate);
		itemProvider.setItemType(String.class);
		itemProvider.setErrorDestination(queue);
		itemProvider.recover("foo", null);
		templateControl.verify();
	}

	public void testGetKeyFromMessage() throws Exception {
		MockControl messageControl = MockControl.createControl(Message.class);
		Message message = (Message) messageControl.getMock();
		messageControl.expectAndReturn(message.getJMSMessageID(), "foo");
		messageControl.replay();

		itemProvider.setItemType(Message.class);
		assertEquals("foo", itemProvider.getKey(message));
		messageControl.verify();

	}

	public void testGetKeyFromNonMessage() throws Exception {
		itemProvider.setItemType(String.class);
		assertEquals("foo", itemProvider.getKey("foo"));
	}

	public void testIsNewForMessage() throws Exception {
		MockControl messageControl = MockControl.createControl(Message.class);
		Message message = (Message) messageControl.getMock();
		messageControl.expectAndReturn(message.getJMSRedelivered(), true);
		messageControl.replay();

		itemProvider.setItemType(Message.class);
		assertEquals(true, itemProvider.hasFailed(message));
		messageControl.verify();
	}

	public void testIsNewForNonMessage() throws Exception {
		itemProvider.setItemType(String.class);
		assertEquals(true, itemProvider.hasFailed("foo"));
	}
}
