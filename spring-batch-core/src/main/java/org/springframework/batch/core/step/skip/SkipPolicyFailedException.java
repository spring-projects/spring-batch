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
package org.springframework.batch.core.step.skip;

import org.springframework.batch.core.UnexpectedJobExecutionException;

/**
 * Special exception to indicate a failure in a skip policy. These need
 * special treatment in the framework in case a skip sends itself into an
 * infinite loop.
 * 
 * @author Dave Syer
 * 
 */
public class SkipPolicyFailedException extends UnexpectedJobExecutionException {

	/**
	 * @param message describes the error to the user
	 * @param ex the exception that was thrown by a {@link SkipPolicy}
	 * @param t the exception that caused the skip
	 */
	public SkipPolicyFailedException(String message, RuntimeException ex, Throwable t) {
		super(message + "\n" + t.getClass().getName() + ": " + t.getMessage(), ex);
	}

}
