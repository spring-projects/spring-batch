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

import org.junit.jupiter.api.Test;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.amqp.AmqpItemWriter;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * <p>
 * Test cases around {@link AmqpItemWriter}.
 * </p>
 *
 * @author Chris Schaefer
 * @author Will Schipp
 * @author Mahmoud Ben Hassine
 */
class AmqpItemWriterTests {

	@Test
	void testNullAmqpTemplate() {
		assertThrows(IllegalArgumentException.class, () -> new AmqpItemWriter<String>(null));
	}

	@Test
	void voidTestWrite() throws Exception {
		AmqpTemplate amqpTemplate = mock();

		amqpTemplate.convertAndSend("foo");

		amqpTemplate.convertAndSend("bar");

		AmqpItemWriter<String> amqpItemWriter = new AmqpItemWriter<>(amqpTemplate);
		amqpItemWriter.write(Chunk.of("foo", "bar"));

	}

}
