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

package org.springframework.batch.io.exception;

/**
 * BatchCritcalException - Indiates to the framework that a critical error has
 * occured and batch processing should immeadiately stop. However, in most cases
 * status should still be persisted indicating that an error foced the job to
 * terminate. Any framework code that catches a BatchCriticalException will
 * rethrow the exception. This allows any code that creates a critical exception
 * to be able to add an error code that will still be accesible at the very
 * beginning of the call chain. (usually a launcher that kicked off the
 * JobController). Error code values 0 - 2000 a reserved for framework classes.
 * Anything greater than 2000 can be used by application code.
 * 
 * @author Lucas Ward
 * 
 */
public class BatchCriticalException extends RuntimeException {
	private static final long serialVersionUID = 8838982304219248527L;

	/**
	 * Constructs a new instance with a default error code of 1.
	 * 
	 * @param msg the exception message.
	 * 
	 */
	public BatchCriticalException(String msg) {
		super(msg);
	}

	/**
	 * Constructs a new instance with a default error code of 1.
	 * 
	 * @param msg the exception message.
	 * 
	 */
	public BatchCriticalException(String msg, Throwable nested) {
		super(msg, nested);
	}

	/**
	 * Constructs a new instance with a nested exception. The error code is
	 * defaulted to 1 and the message is empty.
	 */
	public BatchCriticalException(Throwable nested) {
		super(nested);
	}

	/**
	 * Constructs a new instance, the error code is defaulted to one and the
	 * message is empty.
	 */
	public BatchCriticalException() {
		super();
	}

}
