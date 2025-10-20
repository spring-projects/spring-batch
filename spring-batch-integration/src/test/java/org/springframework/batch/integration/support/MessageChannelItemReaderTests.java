package org.springframework.batch.integration.support;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.MessageChannel;

import static org.mockito.Mockito.mock;

class MessageChannelItemReaderTests {

	@Test
	void testRead() throws Exception {
		// given
		MessageChannel messageChannel = mock();
		MessagingTemplate messagingTemplate = mock();
		Mockito.when(messagingTemplate.receiveAndConvert(messageChannel, String.class)).thenReturn("Foo");
		MessageChannelItemReader<String> reader = new MessageChannelItemReader<>(messagingTemplate, messageChannel,
				String.class);

		// when
		String item = reader.read();

		// then
		Mockito.verify(messagingTemplate, Mockito.times(1)).receiveAndConvert(messageChannel, String.class);
		Assertions.assertEquals("Foo", item);
	}

}