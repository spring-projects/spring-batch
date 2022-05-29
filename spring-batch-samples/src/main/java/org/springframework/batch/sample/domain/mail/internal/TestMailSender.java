/*
 * Copyright 2006-2021 the original author or authors.
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
package org.springframework.batch.sample.domain.mail.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.mail.MessagingException;

import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

/**
 * @author Dan Garrette
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @since 2.1
 */
public class TestMailSender implements MailSender {

	private List<String> subjectsToFail = new ArrayList<>();

	private List<SimpleMailMessage> received = new ArrayList<>();

	public void clear() {
		received.clear();
	}

	@Override
	public void send(SimpleMailMessage simpleMessage) throws MailException {
		throw new UnsupportedOperationException("Not implemented.  Use send(SimpleMailMessage[]).");
	}

	public void setSubjectsToFail(List<String> subjectsToFail) {
		this.subjectsToFail = subjectsToFail;
	}

	@Override
	public void send(SimpleMailMessage... simpleMessages) throws MailException {
		Map<Object, Exception> failedMessages = new LinkedHashMap<>();
		for (SimpleMailMessage simpleMessage : simpleMessages) {
			if (subjectsToFail.contains(simpleMessage.getSubject())) {
				failedMessages.put(simpleMessage, new MessagingException());
			}
			else {
				received.add(simpleMessage);
			}
		}
		if (!failedMessages.isEmpty()) {
			throw new MailSendException(failedMessages);
		}
	}

	public List<SimpleMailMessage> getReceivedMessages() {
		return received;
	}

}
