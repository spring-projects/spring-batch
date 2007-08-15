/*
 * Copyright 2006-2007 the original author or authors.
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

package org.springframework.batch.item.exception;

/**
 * Used to signal an unexpected end of an input or message stream. This is an
 * abnormal condition, not just the end of the data - e.g. if a resource becomes
 * unavailable, or a stream becomes unreadable.
 * 
 * @author Dave Syer
 */
public class UnexpectedInputException extends RuntimeException {

	/**
	 * Generated serial UID.
	 */
	private static final long serialVersionUID = -8325588758094208905L;

	public UnexpectedInputException(String message, Throwable cause) {
		super(message, cause);
	}
}
