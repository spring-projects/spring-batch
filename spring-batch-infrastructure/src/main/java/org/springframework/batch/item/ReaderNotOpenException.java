/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.item;

/**
 * Exception indicating that an {@link ItemReader} needed to be opened before read.
 *
 * @author Ben Hale
 */
@SuppressWarnings("serial")
public class ReaderNotOpenException extends ItemReaderException {

	/**
	 * Create a new {@link ReaderNotOpenException} based on a message.
	 * @param message the message for this exception
	 */
	public ReaderNotOpenException(String message) {
		super(message);
	}

	/**
	 * Create a new {@link ReaderNotOpenException} based on a message and another
	 * exception.
	 * @param msg the message for this exception
	 * @param nested the other exception
	 */
	public ReaderNotOpenException(String msg, Throwable nested) {
		super(msg, nested);
	}

}
