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

/**
 * This class is used to handle errors that occur when email messages are unable
 * to be sent.
 * 
 * @author Dan Garrette
 * @author Dave Syer
 * 
 * @since 2.1
 */
public interface MailErrorHandler {

	/**
	 * This method will be called for each message that failed sending in the
	 * chunk. If the failed message is needed by the handler it will need to be
	 * downcast according to its runtime type. If an exception is thrown from
	 * this method, then it will propagate to the caller.
	 * 
	 * @param message the failed message
	 * @param exception the exception that caused the failure
	 * @throws MailException if the exception cannot be handled
	 */
	public void handle(MailMessage message, Exception exception) throws MailException;

}
