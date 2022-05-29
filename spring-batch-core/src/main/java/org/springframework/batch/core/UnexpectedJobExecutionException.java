/*
 * Copyright 2006-2022 the original author or authors.
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

package org.springframework.batch.core;

/**
 * Indicates to the framework that a critical error has occurred and processing should
 * immediately stop.
 *
 * @author Lucas Ward
 *
 */
public class UnexpectedJobExecutionException extends RuntimeException {

	private static final long serialVersionUID = 8838982304219248527L;

	/**
	 * Constructs a new instance with a message.
	 * @param msg The exception message.
	 *
	 */
	public UnexpectedJobExecutionException(String msg) {
		super(msg);
	}

	/**
	 * Constructs a new instance with a message.
	 * @param msg The exception message.
	 * @param nested An instance of {@link Throwable} that is the cause of the exception.
	 *
	 */
	public UnexpectedJobExecutionException(String msg, Throwable nested) {
		super(msg, nested);
	}

}
