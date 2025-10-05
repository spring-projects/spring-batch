/*
 * Copyright 2017-2025 the original author or authors.
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

package org.springframework.batch.infrastructure.item.amqp.builder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.batch.infrastructure.item.amqp.AmqpItemReader;
import org.springframework.batch.infrastructure.item.amqp.builder.AmqpItemReaderBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Glenn Renfro
 */
@ExtendWith(MockitoExtension.class)
class AmqpItemReaderBuilderTests {

	@Mock
	AmqpTemplate amqpTemplate;

	@Test
	void testNoItemType() {
		when(this.amqpTemplate.receiveAndConvert()).thenReturn("foo");

		final AmqpItemReader<String> amqpItemReader = new AmqpItemReaderBuilder<String>()
			.amqpTemplate(this.amqpTemplate)
			.build();
		assertEquals("foo", amqpItemReader.read());
	}

	@Test
	void testNonMessageItemType() {
		when(this.amqpTemplate.receiveAndConvert()).thenReturn("foo");

		final AmqpItemReader<String> amqpItemReader = new AmqpItemReaderBuilder<String>()
			.amqpTemplate(this.amqpTemplate)
			.itemType(String.class)
			.build();

		assertEquals("foo", amqpItemReader.read());
	}

	@Test
	void testMessageItemType() {
		final Message message = mock();

		when(this.amqpTemplate.receive()).thenReturn(message);

		final AmqpItemReader<Message> amqpItemReader = new AmqpItemReaderBuilder<Message>()
			.amqpTemplate(this.amqpTemplate)
			.itemType(Message.class)
			.build();

		assertEquals(message, amqpItemReader.read());
	}

	@Test
	void testNullAmqpTemplate() {
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> new AmqpItemReaderBuilder<Message>().build());
		assertEquals("amqpTemplate is required.", exception.getMessage());
	}

}
