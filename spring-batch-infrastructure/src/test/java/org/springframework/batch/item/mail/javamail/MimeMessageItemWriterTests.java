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
package org.springframework.batch.item.mail.javamail;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.item.Chunk;
import org.springframework.mail.MailSendException;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.util.ReflectionUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Dave Syer
 * @author Will Schipp
 * @author Mahmoud Ben Hassine
 * @since 2.1
 *
 */
class MimeMessageItemWriterTests {

	private MimeMessageItemWriter writer;

	private final JavaMailSender mailSender = mock();

	private final Session session = Session.getDefaultInstance(new Properties());

	@BeforeEach
	void setUp() {
		writer = new MimeMessageItemWriter(mailSender);
	}

	@Test
	void testSend() {

		MimeMessage foo = new MimeMessage(session);
		MimeMessage bar = new MimeMessage(session);
		MimeMessage[] items = new MimeMessage[] { foo, bar };

		mailSender.send(aryEq(items));

		writer.write(Chunk.of(items));

	}

	@Test
	void testDefaultErrorHandler() {

		MimeMessage foo = new MimeMessage(session);
		MimeMessage bar = new MimeMessage(session);
		MimeMessage[] items = new MimeMessage[] { foo, bar };

		// Spring 4.1 changed the send method to be vargs instead of an array
		if (ReflectionUtils.findMethod(MailSender.class, "send", MimeMessage[].class) != null) {
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

		MimeMessage foo = new MimeMessage(session);
		MimeMessage bar = new MimeMessage(session);
		MimeMessage[] items = new MimeMessage[] { foo, bar };

		// Spring 4.1 changed the send method to be vargs instead of an array
		if (ReflectionUtils.findMethod(MailSender.class, "send", MimeMessage[].class) != null) {
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
