/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.batch.item.xmlpathreader.nls;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.xmlpathreader.exceptions.ReaderRuntimeException;
import org.springframework.util.Assert;

/**
 * Internationalization of exception messages
 * 
 * @author Thomas Nill
 * @since 4.0.1
 *
 */
public class Messages {
	private static final Logger log = LoggerFactory.getLogger(Messages.class);

	private static final String BUNDLE_NAME = "org.springframework.batch.item.xmlpathreader.nls.messages"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	private Messages() {
		super();
	}

	/**
	 * search a message
	 * 
	 * @param messageName the name of the message
	 * @return the massage text
	 */
	public static String getString(String messageName) {
		Assert.hasText(messageName, "The messageName should not be empty");

		try {
			return RESOURCE_BUNDLE.getString(messageName);
		}
		catch (MissingResourceException e) {
			log.error("the message for {} doesnt exist {}", messageName, e);
			return '!' + messageName + '!';
		}
	}

	/**
	 * Throw a IllegalArgumentException
	 * 
	 * @param patternName the name of the message
	 * @param arguments the arguments for the message
	 */
	public static void throwIllegalArgumentException(String patternName, Object... arguments) {
		Assert.hasText(patternName, "The patternName should not be empty");

		String pattern = Messages.getString(patternName);
		throw new IllegalArgumentException(MessageFormat.format(pattern, arguments));
	}

	/**
	 * Throw a ReaderRuntimeException
	 * 
	 * @param cause the cause of the exception
	 * @param patternName the name of the message
	 * @param arguments the arguments for the message
	 */
	public static void throwReaderRuntimeException(Throwable cause, String patternName, Object... arguments) {
		Assert.hasText(patternName, "The patternName should not be empty");

		String pattern = Messages.getString(patternName);
		throw new ReaderRuntimeException(MessageFormat.format(pattern, arguments), cause);
	}

}
