/*
 * Copyright 2006-2023 the original author or authors.
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
package org.springframework.batch.item.mail;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.mail.MessagingException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.item.Chunk;
import org.springframework.mail.MailSendException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.util.ReflectionUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dave Syer
 * @author Will Schipp
 * @author Mahmoud Ben Hassine
 * @since 2.1
 *
 */
class SimpleMailMessageItemWriterTests {

	private final MailSender mailSender = mock();

	private final SimpleMailMessageItemWriter writer = new SimpleMailMessageItemWriter(mailSender);

	@BeforeEach
	void setUp() {
		writer.setMailSender(mailSender);
	}

	@Test
	void testSend() {

		SimpleMailMessage foo = new SimpleMailMessage();
		SimpleMailMessage bar = new SimpleMailMessage();
		SimpleMailMessage[] items = new SimpleMailMessage[] { foo, bar };

		writer.write(Chunk.of(items));

		// Spring 4.1 changed the send method to be vargs instead of an array
		if (ReflectionUtils.findMethod(SimpleMailMessage.class, "send", SimpleMailMessage[].class) != null) {
			verify(mailSender).send(aryEq(items));
		}
		else {
			verify(mailSender).send(items);
		}
	}

	@Test
	void testDefaultErrorHandler() {

		SimpleMailMessage foo = new SimpleMailMessage();
		SimpleMailMessage bar = new SimpleMailMessage();
		SimpleMailMessage[] items = new SimpleMailMessage[] { foo, bar };

		// Spring 4.1 changed the send method to be vargs instead of an array
		if (ReflectionUtils.findMethod(SimpleMailMessage.class, "send", SimpleMailMessage[].class) != null) {
			mailSender.send(aryEq(items));
		}
		else {
			mailSender.send(items);
		}

		when(mailSender).thenThrow(new MailSendException(
				Collections.singletonMap((Object) foo, (Exception) new MessagingException("FOO"))));

		assertThrows(MailSendException.class, () -> writer.write(Chunk.of(items)));
	}

	@Test
	void testCustomErrorHandler() {

		final AtomicReference<String> content = new AtomicReference<>();
		writer.setMailErrorHandler((message, exception) -> content.set(exception.getMessage()));

		SimpleMailMessage foo = new SimpleMailMessage();
		SimpleMailMessage bar = new SimpleMailMessage();
		SimpleMailMessage[] items = new SimpleMailMessage[] { foo, bar };

		// Spring 4.1 changed the send method to be vargs instead of an array
		if (ReflectionUtils.findMethod(SimpleMailMessage.class, "send", SimpleMailMessage[].class) != null) {
			mailSender.send(aryEq(items));
		}
		else {
			mailSender.send(items);
		}

		when(mailSender).thenThrow(new MailSendException(
				Collections.singletonMap((Object) foo, (Exception) new MessagingException("FOO"))));

		writer.write(Chunk.of(items));

		assertEquals("FOO", content.get());
	}

}
