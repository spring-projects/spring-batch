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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import jakarta.jms.Message;

import org.junit.jupiter.api.Test;
import org.springframework.jms.core.JmsOperations;
import org.springframework.jms.core.JmsTemplate;

class JmsItemReaderTests {

	JmsItemReader<String> itemReader;

	@Test
	void testNoItemTypeSunnyDay() {
		JmsOperations jmsTemplate = mock();
		JmsItemReader<String> itemReader = new JmsItemReader<>(jmsTemplate);
		when(jmsTemplate.receiveAndConvert()).thenReturn("foo");

		assertEquals("foo", itemReader.read());
	}

	@Test
	void testSetItemTypeSunnyDay() {
		JmsOperations jmsTemplate = mock();
		JmsItemReader<String> itemReader = new JmsItemReader<>(jmsTemplate);
		when(jmsTemplate.receiveAndConvert()).thenReturn("foo");

		itemReader.setItemType(String.class);
		assertEquals("foo", itemReader.read());
	}

	@Test
	void testSetItemSubclassTypeSunnyDay() {
		JmsOperations jmsTemplate = mock();
		Date date = new java.sql.Date(0L);
		when(jmsTemplate.receiveAndConvert()).thenReturn(date);

		JmsItemReader<Date> itemReader = new JmsItemReader<>(jmsTemplate);
		itemReader.setItemType(Date.class);
		assertEquals(date, itemReader.read());

	}

	@Test
	void testSetItemTypeMismatch() {
		JmsOperations jmsTemplate = mock();
		when(jmsTemplate.receiveAndConvert()).thenReturn("foo");

		JmsItemReader<Date> itemReader = new JmsItemReader<>(jmsTemplate);
		itemReader.setItemType(Date.class);
		Exception exception = assertThrows(IllegalStateException.class, itemReader::read);
		assertTrue(exception.getMessage().contains("wrong type"));
	}

	@Test
	void testNextMessageSunnyDay() {
		JmsOperations jmsTemplate = mock();
		Message message = mock();
		when(jmsTemplate.receive()).thenReturn(message);

		JmsItemReader<Message> itemReader = new JmsItemReader<>(jmsTemplate);
		itemReader.setItemType(Message.class);
		assertEquals(message, itemReader.read());
	}

	@Test
	void testTemplateWithNoDefaultDestination() {
		JmsTemplate jmsTemplate = new JmsTemplate();
		jmsTemplate.setReceiveTimeout(100L);
		assertThrows(IllegalArgumentException.class, () -> new JmsItemReader<>(jmsTemplate));
	}

	@Test
	void testTemplateWithNoTimeout() {
		JmsTemplate jmsTemplate = new JmsTemplate();
		jmsTemplate.setDefaultDestinationName("foo");
		assertThrows(IllegalArgumentException.class, () -> new JmsItemReader<>(jmsTemplate));
	}

}
