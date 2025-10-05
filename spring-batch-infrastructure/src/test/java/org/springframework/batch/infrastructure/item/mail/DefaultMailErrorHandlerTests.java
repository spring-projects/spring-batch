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
package org.springframework.batch.infrastructure.item.mail;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.mail.MessagingException;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.mail.DefaultMailErrorHandler;
import org.springframework.mail.MailException;
import org.springframework.mail.MailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @since 2.1
 *
 */
class DefaultMailErrorHandlerTests {

	private final DefaultMailErrorHandler handler = new DefaultMailErrorHandler();

	/**
	 * Test method for {@link DefaultMailErrorHandler#setMaxMessageLength(int)}.
	 */
	@Test
	void testSetMaxMessageLength() {
		handler.setMaxMessageLength(20);
		SimpleMailMessage mailMessage = new SimpleMailMessage();
		Exception exception = assertThrows(MailException.class,
				() -> handler.handle(mailMessage, new MessagingException()));
		String message = exception.getMessage();
		assertTrue(message.matches(".*SimpleMailMessage: f.*"), "Wrong message: " + message);
	}

	/**
	 * Test method for {@link DefaultMailErrorHandler#handle(MailMessage, Exception)}.
	 */
	@Test
	void testHandle() {
		assertThrows(MailSendException.class, () -> handler.handle(new SimpleMailMessage(), new MessagingException()));
	}

}
