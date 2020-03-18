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

import java.util.Arrays;

import org.junit.Test;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.batch.item.amqp.AmqpItemWriter;

import static org.aspectj.bridge.MessageUtil.fail;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Glenn Renfro
 */
public class AmqpItemWriterBuilderTests {

	@Test
	public void testNullAmqpTemplate() {
		try {
			new AmqpItemWriterBuilder<Message>().build();
			fail("IllegalArgumentException should have been thrown");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("IllegalArgumentException message did not match the expected result.",
					"amqpTemplate is required.", iae.getMessage());
		}
	}

	@Test
	public void voidTestWrite() throws Exception {
		AmqpTemplate amqpTemplate = mock(AmqpTemplate.class);

		AmqpItemWriter<String> amqpItemWriter =
				new AmqpItemWriterBuilder<String>().amqpTemplate(amqpTemplate).build();
		amqpItemWriter.write(Arrays.asList("foo", "bar"));
		verify(amqpTemplate).convertAndSend("foo");
		verify(amqpTemplate).convertAndSend("bar");
	}
}
