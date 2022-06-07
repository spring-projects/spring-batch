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
package org.springframework.batch.item.mail.javamail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.AdditionalMatchers.aryEq;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.mail.MailErrorHandler;
import org.springframework.mail.MailException;
import org.springframework.mail.MailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.util.ReflectionUtils;

/**
 * @author Dave Syer
 * @author Will Schipp
 * @author Mahmoud Ben Hassine
 * @since 2.1
 *
 */
public class MimeMessageItemWriterTests {

	private MimeMessageItemWriter writer = new MimeMessageItemWriter();

	private JavaMailSender mailSender = mock(JavaMailSender.class);

	private Session session = Session.getDefaultInstance(new Properties());

	@BeforeEach
	public void setUp() {
		writer.setJavaMailSender(mailSender);
	}

	@Test
	public void testSend() throws Exception {

		MimeMessage foo = new MimeMessage(session);
		MimeMessage bar = new MimeMessage(session);
		MimeMessage[] items = new MimeMessage[] { foo, bar };

		mailSender.send(aryEq(items));

		writer.write(Arrays.asList(items));

	}

	@Test
	public void testDefaultErrorHandler() {

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

		assertThrows(MailSendException.class, () -> writer.write(List.of(items)));
	}

	@Test
	public void testCustomErrorHandler() throws Exception {

		final AtomicReference<String> content = new AtomicReference<>();
		writer.setMailErrorHandler(new MailErrorHandler() {
			@Override
			public void handle(MailMessage message, Exception exception) throws MailException {
				content.set(exception.getMessage());
			}
		});

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

		writer.write(Arrays.asList(items));

		assertEquals("FOO", content.get());

	}

}
