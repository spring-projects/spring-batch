/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.batch.item.amqp.builder;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.batch.item.amqp.AmqpItemReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Glenn Renfro
 */
public class AmqpItemReaderBuilderTests {

	@Mock
	AmqpTemplate amqpTemplate;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testNoItemType() {
		when(this.amqpTemplate.receiveAndConvert()).thenReturn("foo");

		final AmqpItemReader<String> amqpItemReader = new AmqpItemReaderBuilder<String>()
				.amqpTemplate(this.amqpTemplate).build();
		assertEquals("foo", amqpItemReader.read());
	}

	@Test
	public void testNonMessageItemType() {
		when(this.amqpTemplate.receiveAndConvert()).thenReturn("foo");

		final AmqpItemReader<String> amqpItemReader = new AmqpItemReaderBuilder<String>()
				.amqpTemplate(this.amqpTemplate).itemType(String.class).build();

		assertEquals("foo", amqpItemReader.read());
	}

	@Test
	public void testMessageItemType() {
		final Message message = mock(Message.class);

		when(this.amqpTemplate.receive()).thenReturn(message);

		final AmqpItemReader<Message> amqpItemReader = new AmqpItemReaderBuilder<Message>()
				.amqpTemplate(this.amqpTemplate).itemType(Message.class).build();

		assertEquals(message, amqpItemReader.read());
	}

	@Test
	public void testNullAmqpTemplate() {
		try {
			new AmqpItemReaderBuilder<Message>().build();
			fail("IllegalArgumentException should have been thrown");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("IllegalArgumentException message did not match the expected result.",
					"amqpTemplate is required.", iae.getMessage());
		}
	}
}
