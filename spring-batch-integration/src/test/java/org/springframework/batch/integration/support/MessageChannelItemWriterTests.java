/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.batch.integration.support;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.MessageChannel;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MessageChannelItemWriter}.
 *
 * @author Mahmoud Ben Hassine
 */
class MessageChannelItemWriterTests {

	@Test
	void testWrite() throws Exception {
		// given
		MessageChannel messageChannel = mock();
		MessagingTemplate messagingTemplate = mock();
		MessageChannelItemWriter<String> itemWriter = new MessageChannelItemWriter<>(messagingTemplate, messageChannel);
		Chunk<String> items = Chunk.of("foo", "bar");

		// when
		itemWriter.write(items);

		// then
		Mockito.verify(messagingTemplate, Mockito.times(2)).send(eq(messageChannel), Mockito.any());
	}

}