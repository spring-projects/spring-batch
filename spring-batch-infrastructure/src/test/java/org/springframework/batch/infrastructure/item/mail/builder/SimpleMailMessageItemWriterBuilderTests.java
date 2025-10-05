/*
 * Copyright 2017-2023 the original author or authors.
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

package org.springframework.batch.infrastructure.item.mail.builder;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.mail.MessagingException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.mail.SimpleMailMessageItemWriter;
import org.springframework.batch.infrastructure.item.mail.builder.SimpleMailMessageItemWriterBuilder;
import org.springframework.mail.MailSendException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 */
class SimpleMailMessageItemWriterBuilderTests {

	private MailSender mailSender;

	private SimpleMailMessage foo;

	private SimpleMailMessage bar;

	private SimpleMailMessage[] items;

	@BeforeEach
	void setup() {
		mailSender = mock();
		this.foo = new SimpleMailMessage();
		this.bar = new SimpleMailMessage();
		this.items = new SimpleMailMessage[] { this.foo, this.bar };
	}

	@Test
	void testSend() {
		SimpleMailMessageItemWriter writer = new SimpleMailMessageItemWriterBuilder().mailSender(this.mailSender)
			.build();

		writer.write(Chunk.of(this.items));
		verify(this.mailSender).send(this.foo, this.bar);
	}

	@Test
	void testMailSenderNotSet() {
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> new SimpleMailMessageItemWriterBuilder().build());
		assertEquals("A mailSender is required", exception.getMessage());
	}

	@Test
	void testErrorHandler() {
		SimpleMailMessageItemWriter writer = new SimpleMailMessageItemWriterBuilder().mailSender(this.mailSender)
			.build();

		this.mailSender.send(this.foo, this.bar);
		when(this.mailSender)
			.thenThrow(new MailSendException(Collections.singletonMap(this.foo, new MessagingException("FOO"))));
		assertThrows(MailSendException.class, () -> writer.write(Chunk.of(this.items)));
	}

	@Test
	void testCustomErrorHandler() {
		final AtomicReference<String> content = new AtomicReference<>();
		SimpleMailMessageItemWriter writer = new SimpleMailMessageItemWriterBuilder()
			.mailErrorHandler((message, exception) -> content.set(exception.getMessage()))
			.mailSender(this.mailSender)
			.build();

		this.mailSender.send(this.foo, this.bar);
		when(this.mailSender)
			.thenThrow(new MailSendException(Collections.singletonMap(this.foo, new MessagingException("FOO"))));
		writer.write(Chunk.of(this.items));
		assertEquals("FOO", content.get());
	}

}
