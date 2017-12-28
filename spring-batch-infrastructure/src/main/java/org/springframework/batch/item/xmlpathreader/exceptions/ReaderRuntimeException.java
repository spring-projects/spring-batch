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

package org.springframework.batch.item.xmlpathreader.exceptions;

import org.springframework.util.Assert;

/**
 * class for runtime exception of the reader
 * 
 * @author Thomas Nill
 * @since 4.0.1
 *
 */
public class ReaderRuntimeException extends RuntimeException {

	/**
	 * Constructor
	 * 
	 * @param cause the cause of the exception
	 */
	public ReaderRuntimeException(Throwable cause) {
		super(cause);
		Assert.notNull(cause, "The cause should not be null");

	}

	/**
	 * Constructor
	 * 
	 * @param message the message the explains the exception 
	 * @param cause the cause of the exception
	 */
	public ReaderRuntimeException(String message, Throwable cause) {
		super(message, cause);
		Assert.notNull(cause, "The cause should not be null");
		Assert.hasText(message, "The message should not be empty");
	}

}
