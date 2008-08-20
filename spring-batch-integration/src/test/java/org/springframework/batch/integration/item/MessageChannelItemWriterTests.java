/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.integration.item;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.ThreadLocalChannel;
import org.springframework.integration.endpoint.DefaultEndpoint;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageTarget;
import org.springframework.util.ReflectionUtils;

/**
 * @author Dave Syer
 *
 */
public class MessageChannelItemWriterTests {

	/**
	 * Test method for {@link org.springframework.batch.integration.item.MessageChannelItemWriter#setChannel(org.springframework.integration.channel.MessageChannel)}.
	 */
	@Test
	public void testSetChannel() {
		Method method = ReflectionUtils.findMethod(MessageChannelItemWriter.class, "setChannel", new Class<?>[] {MessageChannel.class});
		assertNotNull(method);
		Annotation[] annotations = AnnotationUtils.getAnnotations(method);
		assertEquals(1, annotations.length);
		assertEquals(Required.class, annotations[0].annotationType());
	}

	@Test
	public void testWrite() throws Exception {
		DirectChannel channel = new DirectChannel();
		ThreadLocalChannel receiver = new ThreadLocalChannel();
		channel.subscribe(receiver);
		MessageChannelItemWriter<String> writer = new MessageChannelItemWriter<String>();
		writer.setChannel(channel);
		writer.write(Collections.singletonList("foo"));
		Message<?> message = receiver.receive(10);
		assertNotNull(message);
		assertEquals("foo", message.getPayload());
	}

	@Test
	public void testWriteWithRollback() throws Exception {
		DirectChannel channel = new DirectChannel();
		channel.subscribe(new MessageTarget() {
			public boolean send(Message<?> message) {
				throw new RuntimeException("Planned failure");
			}
		});
		MessageChannelItemWriter<String> writer = new MessageChannelItemWriter<String>();
		writer.setChannel(channel);
		try {
			writer.write(Collections.singletonList("foo"));
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("Planned failure", e.getMessage());
		}
	}

	@Test
	public void testWriteWithRollbackOnEndpoint() throws Exception {
		DirectChannel channel = new DirectChannel();
		DefaultEndpoint<MessageHandler> endpoint = new DefaultEndpoint<MessageHandler>(new MessageHandler() {
			public Message<?> handle(Message<?> message) {
				throw new RuntimeException("Planned failure");
			}
		});
		channel.subscribe(endpoint);
		MessageChannelItemWriter<String> writer = new MessageChannelItemWriter<String>();
		writer.setChannel(channel);
		try {
			writer.write(Collections.singletonList("foo"));
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			// INT-377: this assertion fails because the exception is wrapped too tightly
			assertEquals("Planned failure", e.getCause().getMessage());
		}
	}
}
