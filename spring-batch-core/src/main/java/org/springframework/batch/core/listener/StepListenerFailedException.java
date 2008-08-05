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
package org.springframework.batch.core.listener;


/**
 * Exception to indicate a problem in a step listener.
 * 
 * @author Dave Syer
 *
 */
public class StepListenerFailedException extends RuntimeException {

	/**
	 * @param message describes the error to the user
	 * @param t the exception that was thrown by a listener
	 */
	public StepListenerFailedException(String message, Throwable t) {
		super(message, t);
	}

	/**
	 * @param message describes the error to the user
	 * @param ex the exception that was thrown by a listener
	 * @param e the exception that caused the skip
	 */
	public StepListenerFailedException(String message, Throwable ex, RuntimeException e) {
		super(message + "\n" + e.getClass().getName() + ": " + e.getMessage(), ex);
	}

}
