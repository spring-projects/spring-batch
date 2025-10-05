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

package org.springframework.batch.infrastructure.item.jms;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.jms.JmsItemWriter;
import org.springframework.jms.core.JmsOperations;
import org.springframework.jms.core.JmsTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class JmsItemWriterTests {

	@Test
	void testNoItemTypeSunnyDay() throws Exception {
		JmsOperations jmsTemplate = mock();
		jmsTemplate.convertAndSend("foo");
		jmsTemplate.convertAndSend("bar");

		JmsItemWriter<String> itemWriter = new JmsItemWriter<>(jmsTemplate);
		itemWriter.write(Chunk.of("foo", "bar"));
	}

	@Test
	void testTemplateWithNoDefaultDestination() {
		JmsTemplate jmsTemplate = new JmsTemplate();
		assertThrows(IllegalArgumentException.class, () -> new JmsItemWriter<>(jmsTemplate));
	}

}
