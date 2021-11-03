/*
 * Copyright 2012-2013 the original author or authors.
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
package org.springframework.batch.core.job.builder;

/**
 * @author Dave Syer
 * 
 * @since 2.2
 *
 */
@SuppressWarnings("serial")
public class FlowBuilderException extends RuntimeException {

	/**
	 * Constructor that accepts a message and the cause.
	 * @param msg {@link String} containing the message for the {@link Exception}.
	 * @param e the cause of the exception.
	 */
	public FlowBuilderException(String msg, Exception e) {
		super(msg, e);
	}

	/**
	 * Constructor that accepts the cause of the {@link Exception}.
	 * @param e the cause of the exception.
	 */
	public FlowBuilderException(Exception e) {
		super(e);
	}

	/**
	 * Constructor that accepts a message for this {@link Exception}.
	 * @param msg {@link String} containing the message for the {@link Exception}.
	 */
	public FlowBuilderException(String msg) {
		super(msg);
	}

}
