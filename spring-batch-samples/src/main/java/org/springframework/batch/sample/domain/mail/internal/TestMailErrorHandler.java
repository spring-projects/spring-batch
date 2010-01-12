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
package org.springframework.batch.sample.domain.mail.internal;

import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;

import org.springframework.batch.item.mail.MailErrorHandler;
import org.springframework.mail.MailMessage;
import org.springframework.mail.SimpleMailMessage;

/**
 * This handler prints out failed messages with their exceptions. It also
 * maintains a list of all failed messages it receives for lookup later by an
 * assertion.
 * 
 * @author Dan Garrette
 * @author Dave Syer
 * 
 * @since 2.1
 */
public class TestMailErrorHandler implements MailErrorHandler {

	private List<SimpleMailMessage> failedMessages = new ArrayList<SimpleMailMessage>();

	public void handle(MailMessage failedMessage, MessagingException ex) {
		this.failedMessages.add(failedMessage);
		System.out.println("Mail message failed: " + failedMessage);
		System.out.println(ex);
	}

	public List<SimpleMailMessage> getFailedMessages() {
		return failedMessages;
	}

	public void clear() {
		this.failedMessages.clear();
	}
}
