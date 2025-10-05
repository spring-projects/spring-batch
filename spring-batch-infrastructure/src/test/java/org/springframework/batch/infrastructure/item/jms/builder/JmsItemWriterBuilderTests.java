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

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.jms.JmsItemWriter;
import org.springframework.batch.infrastructure.item.jms.builder.JmsItemWriterBuilder;
import org.springframework.jms.core.JmsOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 */
class JmsItemWriterBuilderTests {

	@Test
	void testNoItem() throws Exception {
		JmsOperations jmsTemplate = mock();
		JmsItemWriter<String> itemWriter = new JmsItemWriterBuilder<String>().jmsTemplate(jmsTemplate).build();
		ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
		itemWriter.write(Chunk.of("foo", "bar"));
		verify(jmsTemplate, times(2)).convertAndSend(argCaptor.capture());
		assertEquals("foo", argCaptor.getAllValues().get(0), "Expected foo");
		assertEquals("bar", argCaptor.getAllValues().get(1), "Expected bar");
	}

	@Test
	void testNullJmsTemplate() {
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> new JmsItemWriterBuilder<String>().build());
		assertEquals("jmsTemplate is required.", exception.getMessage());
	}

}
