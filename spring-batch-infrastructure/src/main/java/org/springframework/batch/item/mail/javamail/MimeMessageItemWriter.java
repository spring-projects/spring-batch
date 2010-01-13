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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.mail.internet.MimeMessage;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.mail.DefaultMailErrorHandler;
import org.springframework.batch.item.mail.MailErrorHandler;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.util.Assert;

/**
 * <p>
 * A simple {@link ItemWriter} that can send mail messages. If it fails there is
 * no guarantee about which of the messages were sent, but the ones that failed
 * can be picked up in the error handler. Because the mail protocol is not
 * transactional, failures should be dealt with here if possible rather than
 * allowing them to be rethrown (which is the default).
 * </p>
 * 
 * <p>
 * Delegates the actual sending of messages to a {@link JavaMailSender}, using the
 * batch method {@link JavaMailSender#send(MimeMessage[])}, which normally uses
 * a single server connection for the whole batch (depending on the
 * implementation). The efficiency of for large volumes of messages (repeated
 * calls to the item writer) might be improved by the use of a special
 * {@link JavaMailSender} that caches connections to the server in between
 * calls.
 * </p>
 * 
 * <p>
 * Stateless, so automatically restartable.
 * </p>
 * 
 * @author Dave Syer
 * 
 * @since 2.1
 * 
 */
public class MimeMessageItemWriter implements ItemWriter<MimeMessage> {

	private JavaMailSender mailSender;

	private MailErrorHandler mailErrorHandler = new DefaultMailErrorHandler();

	/**
	 * A {@link JavaMailSender} to be used to send messages in {@link #write(List)}.
	 * 
	 * @param mailSender
	 */
	public void setJavaMailSender(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	/**
	 * The handler for failed messages. Defaults to a
	 * {@link DefaultMailErrorHandler}.
	 * 
	 * @param mailErrorHandler the mail error handler to set
	 */
	public void setMailErrorHandler(MailErrorHandler mailErrorHandler) {
		this.mailErrorHandler = mailErrorHandler;
	}

	/**
	 * Check mandatory properties (mailSender).
	 * 
	 * @throws IllegalStateException if the mandatory properties are not set
	 * 
	 * @see InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws IllegalStateException {
		Assert.state(mailSender != null, "A MailSender must be provided.");
	}

	/**
	 * @param items the items to send
	 * @see ItemWriter#write(List)
	 */
	public void write(List<? extends MimeMessage> items) throws MailException {
		try {
			mailSender.send(items.toArray(new MimeMessage[items.size()]));
		}
		catch (MailSendException e) {
			@SuppressWarnings("unchecked")
			Map<Object, Exception> failedMessages = e.getFailedMessages();
			for (Entry<Object, Exception> entry : failedMessages.entrySet()) {
				mailErrorHandler.handle(new MimeMailMessage((MimeMessage)entry.getKey()), entry.getValue());
			}
		}
	}

}
