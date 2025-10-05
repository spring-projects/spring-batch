/*
 * Copyright 2017-2022 the original author or authors.
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

package org.springframework.batch.infrastructure.item.jms.builder;

import java.util.Date;

import jakarta.jms.Message;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.jms.JmsItemReader;
import org.springframework.batch.infrastructure.item.jms.builder.JmsItemReaderBuilder;
import org.springframework.jms.core.JmsOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 */
class JmsItemReaderBuilderTests {

	private JmsOperations defaultJmsTemplate;

	@BeforeEach
	void setupJmsTemplate() {
		this.defaultJmsTemplate = mock();
		when(this.defaultJmsTemplate.receiveAndConvert()).thenReturn("foo");
	}

	@Test
	void testBasicRead() {
		JmsItemReader<String> itemReader = new JmsItemReaderBuilder<String>().jmsTemplate(this.defaultJmsTemplate)
			.build();
		assertEquals("foo", itemReader.read());
	}

	@Test
	void testSetItemSubclassType() {
		JmsOperations jmsTemplate = mock();

		Date date = new java.sql.Date(0L);
		when(jmsTemplate.receiveAndConvert()).thenReturn(date);

		JmsItemReader<Date> itemReader = new JmsItemReaderBuilder<Date>().jmsTemplate(jmsTemplate)
			.itemType(Date.class)
			.build();
		assertEquals(date, itemReader.read());
	}

	@Test
	void testSetItemTypeMismatch() {
		JmsItemReader<Date> itemReader = new JmsItemReaderBuilder<Date>().jmsTemplate(this.defaultJmsTemplate)
			.itemType(Date.class)
			.build();
		Exception exception = assertThrows(IllegalStateException.class, itemReader::read);
		assertTrue(exception.getMessage().contains("wrong type"));
	}

	@Test
	void testMessageType() {
		JmsOperations jmsTemplate = mock();
		Message message = mock();
		when(jmsTemplate.receive()).thenReturn(message);

		JmsItemReader<Message> itemReader = new JmsItemReaderBuilder<Message>().jmsTemplate(jmsTemplate)
			.itemType(Message.class)
			.build();
		assertEquals(message, itemReader.read());
	}

	@Test
	void testNullJmsTemplate() {
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> new JmsItemReaderBuilder<String>().itemType(String.class).build());
		assertEquals("jmsTemplate is required.", exception.getMessage());
	}

}
