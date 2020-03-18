/*
 * Copyright 2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.item.mail.builder;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import javax.mail.MessagingException;

import org.junit.Before;
import org.junit.Test;

import org.springframework.batch.item.mail.MailErrorHandler;
import org.springframework.batch.item.mail.SimpleMailMessageItemWriter;
import org.springframework.mail.MailException;
import org.springframework.mail.MailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Glenn Renfro
 */
public class SimpleMailMessageItemWriterBuilderTests {

	private MailSender mailSender;

	private SimpleMailMessage foo;

	private SimpleMailMessage bar;

	private SimpleMailMessage[] items;

	@Before
	public void setup() {
		mailSender = mock(MailSender.class);
		this.foo = new SimpleMailMessage();
		this.bar = new SimpleMailMessage();
		this.items = new SimpleMailMessage[] { this.foo, this.bar };
	}

	@Test
	public void testSend() throws Exception {
		SimpleMailMessageItemWriter writer = new SimpleMailMessageItemWriterBuilder().mailSender(this.mailSender)
				.build();

		writer.write(Arrays.asList(this.items));
		verify(this.mailSender).send(this.foo, this.bar);
	}

	@Test
	public void testMailSenderNotSet() throws Exception {
		try {
			new SimpleMailMessageItemWriterBuilder().build();
			fail("A mailSender is required");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("A mailSender is required", iae.getMessage());
		}
	}

	@Test(expected = MailSendException.class)
	public void testErrorHandler() throws Exception {
		SimpleMailMessageItemWriter writer = new SimpleMailMessageItemWriterBuilder().mailSender(this.mailSender)
				.build();

		this.mailSender.send(this.foo, this.bar);
		when(this.mailSender)
				.thenThrow(new MailSendException(Collections.singletonMap(this.foo, new MessagingException("FOO"))));
		writer.write(Arrays.asList(this.items));
	}

	@Test
	public void testCustomErrorHandler() throws Exception {
		final AtomicReference<String> content = new AtomicReference<>();
		SimpleMailMessageItemWriter writer = new SimpleMailMessageItemWriterBuilder()
				.mailErrorHandler(new MailErrorHandler() {
					@Override
					public void handle(MailMessage message, Exception exception) throws MailException {
						content.set(exception.getMessage());
					}
				}).mailSender(this.mailSender).build();

		this.mailSender.send(this.foo, this.bar);
		when(this.mailSender)
				.thenThrow(new MailSendException(Collections.singletonMap(this.foo, new MessagingException("FOO"))));
		writer.write(Arrays.asList(this.items));
		assertEquals("FOO", content.get());
	}

}
