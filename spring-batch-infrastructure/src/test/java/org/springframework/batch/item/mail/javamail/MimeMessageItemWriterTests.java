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
package org.springframework.batch.item.mail.javamail;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.mail.MailErrorHandler;
import org.springframework.mail.MailException;
import org.springframework.mail.MailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * @author Dave Syer
 * 
 * @since 2.1
 * 
 */
public class MimeMessageItemWriterTests {

	private MimeMessageItemWriter writer = new MimeMessageItemWriter();

	private JavaMailSender mailSender = EasyMock.createMock(JavaMailSender.class);
	
	private Session session = Session.getDefaultInstance(new Properties());

	@Before
	public void setUp() {
		writer.setJavaMailSender(mailSender);
	}

	@Test
	public void testSend() throws Exception {

		MimeMessage foo = new MimeMessage(session);
		MimeMessage bar = new MimeMessage(session);
		MimeMessage[] items = new MimeMessage[] { foo, bar };

		mailSender.send(EasyMock.aryEq(items));
		EasyMock.expectLastCall();
		EasyMock.replay(mailSender);

		writer.write(Arrays.asList(items));

		EasyMock.verify(mailSender);

	}

	@Test(expected = MailSendException.class)
	public void testDefaultErrorHandler() throws Exception {

		MimeMessage foo = new MimeMessage(session);
		MimeMessage bar = new MimeMessage(session);
		MimeMessage[] items = new MimeMessage[] { foo, bar };

		mailSender.send(EasyMock.aryEq(items));
		EasyMock.expectLastCall().andThrow(
				new MailSendException(Collections.singletonMap((Object)foo, (Exception)new MessagingException("FOO"))));
		EasyMock.replay(mailSender);

		writer.write(Arrays.asList(items));

		EasyMock.verify(mailSender);

	}

	@Test
	public void testCustomErrorHandler() throws Exception {

		final AtomicReference<String> content = new AtomicReference<String>();
		writer.setMailErrorHandler(new MailErrorHandler() {
			public void handle(MailMessage message, Exception exception) throws MailException {
				content.set(exception.getMessage());
			}
		});

		MimeMessage foo = new MimeMessage(session);
		MimeMessage bar = new MimeMessage(session);
		MimeMessage[] items = new MimeMessage[] { foo, bar };

		mailSender.send(EasyMock.aryEq(items));
		EasyMock.expectLastCall().andThrow(
				new MailSendException(Collections.singletonMap((Object)foo, (Exception) new MessagingException("FOO"))));
		EasyMock.replay(mailSender);

		writer.write(Arrays.asList(items));

		assertEquals("FOO", content.get());

		EasyMock.verify(mailSender);

	}

}
