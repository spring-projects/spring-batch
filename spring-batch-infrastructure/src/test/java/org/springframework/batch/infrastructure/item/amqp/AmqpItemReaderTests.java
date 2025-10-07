/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.batch.infrastructure.item.amqp;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.batch.infrastructure.item.amqp.AmqpItemReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>
 * Test cases around {@link AmqpItemReader}.
 * </p>
 *
 * @author Chris Schaefer
 * @author Will Schipp
 */
class AmqpItemReaderTests {

	@Test
	void testNullAmqpTemplate() {
		assertThrows(IllegalArgumentException.class, () -> new AmqpItemReader<String>(null));
	}

	@Test
	void testNoItemType() {
		final AmqpTemplate amqpTemplate = mock();
		when(amqpTemplate.receiveAndConvert()).thenReturn("foo");

		final AmqpItemReader<String> amqpItemReader = new AmqpItemReader<>(amqpTemplate);
		assertEquals("foo", amqpItemReader.read());
	}

	@Test
	void testNonMessageItemType() {
		final AmqpTemplate amqpTemplate = mock();
		when(amqpTemplate.receiveAndConvert()).thenReturn("foo");

		final AmqpItemReader<String> amqpItemReader = new AmqpItemReader<>(amqpTemplate);
		amqpItemReader.setItemType(String.class);

		assertEquals("foo", amqpItemReader.read());

	}

	@Test
	void testMessageItemType() {
		final AmqpTemplate amqpTemplate = mock();
		final Message message = mock();

		when(amqpTemplate.receive()).thenReturn(message);

		final AmqpItemReader<Message> amqpItemReader = new AmqpItemReader<>(amqpTemplate);
		amqpItemReader.setItemType(Message.class);

		assertEquals(message, amqpItemReader.read());

	}

	@Test
	void testTypeMismatch() {
		final AmqpTemplate amqpTemplate = mock();

		when(amqpTemplate.receiveAndConvert()).thenReturn("foo");

		final AmqpItemReader<Integer> amqpItemReader = new AmqpItemReader<>(amqpTemplate);
		amqpItemReader.setItemType(Integer.class);

		Exception exception = assertThrows(IllegalStateException.class, amqpItemReader::read);
		assertTrue(exception.getMessage().contains("wrong type"));

	}

	@Test
	void testNullItemType() {
		final AmqpTemplate amqpTemplate = mock();

		final AmqpItemReader<String> amqpItemReader = new AmqpItemReader<>(amqpTemplate);
		assertThrows(IllegalArgumentException.class, () -> amqpItemReader.setItemType(null));
	}

}
