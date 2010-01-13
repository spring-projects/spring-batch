/*
 * Copyright 2006-2010 the original author or authors.
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
package org.springframework.batch.item.mail;

import static org.junit.Assert.*;

import javax.mail.MessagingException;

import org.junit.Test;
import org.springframework.mail.MailException;
import org.springframework.mail.MailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;

/**
 * @author Dave Syer
 * 
 * @since 2.1
 *
 */
public class DefaultMailErrorHandlerTests {
	
	private DefaultMailErrorHandler handler = new DefaultMailErrorHandler();

	/**
	 * Test method for {@link DefaultMailErrorHandler#setMaxMessageLength(int)}.
	 */
	@Test
	public void testSetMaxMessageLength() {
		handler.setMaxMessageLength(20);
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			handler.handle(message, new MessagingException());
			fail("Expected MailException");
		} catch (MailException e) {
			String msg = e.getMessage();
			assertTrue("Wrong message: "+msg, msg.matches(".*SimpleMailMessage: f;.*"));
		}
	}

	/**
	 * Test method for {@link DefaultMailErrorHandler#handle(MailMessage, Exception)}.
	 */
	@Test(expected=MailSendException.class)
	public void testHandle() {
		handler.handle(new SimpleMailMessage(), new MessagingException());
	}

}
