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
package org.springframework.batch.retry.policy;

import org.springframework.batch.retry.RetryException;

/**
 * Exception that indicates that a cache limit was exceeded. This is often a
 * sign of badly or inconsistently implemented hashCode, equals in failed items.
 * Items can then fail repeatedly and appear different to the cache, so they get
 * added over and over again until a limit is reached and this exception is
 * thrown. Consult the documentation of the {@link RetryContextCache} in use to
 * determine how to increase the limit if appropriate.
 * 
 * @author Dave Syer
 * 
 */
public class RetryCacheCapacityExceededException extends RetryException {

	/**
	 * Constructs a new instance with a message.
	 * 
	 * @param message
	 */
	public RetryCacheCapacityExceededException(String message) {
		super(message);
	}

	/**
	 * Constructs a new instance with a message and nested exception.
	 * 
	 * @param msg the exception message.
	 * 
	 */
	public RetryCacheCapacityExceededException(String msg, Throwable nested) {
		super(msg, nested);
	}

}
