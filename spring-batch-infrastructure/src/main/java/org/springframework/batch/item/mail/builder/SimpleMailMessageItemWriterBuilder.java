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

import java.util.List;

import org.springframework.batch.item.mail.DefaultMailErrorHandler;
import org.springframework.batch.item.mail.MailErrorHandler;
import org.springframework.batch.item.mail.SimpleMailMessageItemWriter;
import org.springframework.mail.MailSender;
import org.springframework.util.Assert;

/**
 * Creates a fully qualified SimpleMailMessageItemWriter.
 *
 * @author Glenn Renfro
 *
 * @since 4.0
 */

public class SimpleMailMessageItemWriterBuilder {

	private MailSender mailSender;

	private MailErrorHandler mailErrorHandler = new DefaultMailErrorHandler();

	/**
	 * A {@link MailSender} to be used to send messages in
	 * {@link SimpleMailMessageItemWriter#write(List)}.
	 *
	 * @param mailSender strategy for sending simple mails.
	 * @return this instance for method chaining.
	 * @see SimpleMailMessageItemWriter#setMailSender(MailSender)
	 */
	public SimpleMailMessageItemWriterBuilder mailSender(MailSender mailSender) {
		this.mailSender = mailSender;
		return this;
	}

	/**
	 * The handler for failed messages. Defaults to a {@link DefaultMailErrorHandler}.
	 *
	 * @param mailErrorHandler the mail error handler to set.
	 * @return this instance for method chaining.
	 * @see SimpleMailMessageItemWriter#setMailErrorHandler(MailErrorHandler)
	 */
	public SimpleMailMessageItemWriterBuilder mailErrorHandler(MailErrorHandler mailErrorHandler) {
		this.mailErrorHandler = mailErrorHandler;
		return this;
	}

	/**
	 * Returns a fully constructed {@link SimpleMailMessageItemWriter}.
	 *
	 * @return a new {@link SimpleMailMessageItemWriter}
	 */
	public SimpleMailMessageItemWriter build() {
		Assert.notNull(this.mailSender, "A mailSender is required");

		SimpleMailMessageItemWriter writer = new SimpleMailMessageItemWriter();
		writer.setMailSender(this.mailSender);
		if (mailErrorHandler != null) {
			writer.setMailErrorHandler(this.mailErrorHandler);
		}

		return writer;
	}
}
