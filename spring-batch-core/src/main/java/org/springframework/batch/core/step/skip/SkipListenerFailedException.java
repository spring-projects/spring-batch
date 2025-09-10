/*
 * Copyright 2006-2025 the original author or authors.
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
package org.springframework.batch.core.step.skip;

import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.core.job.UnexpectedJobExecutionException;

/**
 * Special exception to indicate a failure in a skip listener. These need special
 * treatment in the framework in case a skip sends itself into an infinite loop.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class SkipListenerFailedException extends UnexpectedJobExecutionException {

	/**
	 * Create a new {@link SkipListenerFailedException}.
	 * @param message the exception message
	 * @param throwable the error that was thrown by a {@link SkipListener}
	 * @since 6.0
	 */
	public SkipListenerFailedException(String message, Throwable throwable) {
		super(message, throwable);
	}

	/**
	 * @param message describes the error to the user
	 * @param ex the exception that was thrown by a {@link SkipListener}
	 * @param t the exception that caused the skip
	 */
	public SkipListenerFailedException(String message, RuntimeException ex, Throwable t) {
		super(message + "\n" + t.getClass().getName() + ": " + t.getMessage(), ex);
	}

}
