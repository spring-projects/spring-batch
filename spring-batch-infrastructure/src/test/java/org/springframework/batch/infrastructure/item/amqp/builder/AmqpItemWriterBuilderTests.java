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

package org.springframework.batch.infrastructure.item.amqp.builder;

import org.junit.jupiter.api.Test;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.amqp.AmqpItemWriter;
import org.springframework.batch.infrastructure.item.amqp.builder.AmqpItemWriterBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 */
class AmqpItemWriterBuilderTests {

	@Test
	void testNullAmqpTemplate() {
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> new AmqpItemWriterBuilder<Message>().build());
		assertEquals("amqpTemplate is required.", exception.getMessage());
	}

	@Test
	void voidTestWrite() throws Exception {
		AmqpTemplate amqpTemplate = mock();

		AmqpItemWriter<String> amqpItemWriter = new AmqpItemWriterBuilder<String>().amqpTemplate(amqpTemplate).build();
		amqpItemWriter.write(Chunk.of("foo", "bar"));
		verify(amqpTemplate).convertAndSend("foo");
		verify(amqpTemplate).convertAndSend("bar");
	}

}
