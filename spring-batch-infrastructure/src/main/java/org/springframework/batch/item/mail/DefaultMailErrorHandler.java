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

import org.springframework.mail.MailException;
import org.springframework.mail.MailMessage;
import org.springframework.mail.MailSendException;

/**
 * This {@link MailErrorHandler} implementation simply rethrows the exception it
 * receives.
 * 
 * @author Dan Garrette
 * @author Dave Syer
 * 
 * @since 2.1
 */
public class DefaultMailErrorHandler implements MailErrorHandler {

	private static final int DEFAULT_MAX_MESSAGE_LENGTH = 1024;

	private int maxMessageLength = DEFAULT_MAX_MESSAGE_LENGTH;

	/**
	 * The limit for the size of message that will be copied to the exception
	 * message. Output will be truncated beyond that. Default value is 1024.
	 * 
	 * @param maxMessageLength the maximum message length
	 */
	public void setMaxMessageLength(int maxMessageLength) {
		this.maxMessageLength = maxMessageLength;
	}

	/**
	 * Wraps the input exception with a runtime {@link MailException}. The
	 * exception message will contain the failed message (using toString).
	 * 
	 * @param message a failed message
	 * @param exception a MessagingException
	 * @throws MailException a translation of the Exception
	 * @see MailErrorHandler#handle(MailMessage, Exception)
	 */
	public void handle(MailMessage message, Exception exception) throws MailException {
		String msg = message.toString();
		throw new MailSendException("Mail server send failed: "
				+ msg.substring(0, Math.min(maxMessageLength, msg.length())), exception);
	}
}
