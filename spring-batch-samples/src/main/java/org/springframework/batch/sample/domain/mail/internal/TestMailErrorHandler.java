/*
 * Copyright 2006-2014 the original author or authors.
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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.mail.MailErrorHandler;
import org.springframework.mail.MailMessage;

/**
 * This handler prints out failed messages with their exceptions. It also maintains a list
 * of all failed messages it receives for lookup later by an assertion.
 *
 * @author Dan Garrette
 * @author Dave Syer
 * @since 2.1
 */
public class TestMailErrorHandler implements MailErrorHandler {

	private static final Log LOGGER = LogFactory.getLog(TestMailErrorHandler.class);

	private List<MailMessage> failedMessages = new ArrayList<>();

	@Override
	public void handle(MailMessage failedMessage, Exception ex) {
		this.failedMessages.add(failedMessage);
		LOGGER.error("Mail message failed: " + failedMessage, ex);
	}

	public List<MailMessage> getFailedMessages() {
		return failedMessages;
	}

	public void clear() {
		this.failedMessages.clear();
	}

}
