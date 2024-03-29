/*
 * Copyright 2006-2023 the original author or authors.
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

package org.springframework.batch.repeat.exception;

import java.util.Arrays;

import org.springframework.batch.repeat.RepeatContext;

/**
 * Composite {@link ExceptionHandler} that loops though a list of delegates.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class CompositeExceptionHandler implements ExceptionHandler {

	private ExceptionHandler[] handlers = new ExceptionHandler[0];

	public void setHandlers(ExceptionHandler[] handlers) {
		this.handlers = Arrays.asList(handlers).toArray(new ExceptionHandler[handlers.length]);
	}

	/**
	 * Iterate over the handlers delegating the call to each in turn. The chain ends if an
	 * exception is thrown.
	 *
	 * @see ExceptionHandler#handleException(RepeatContext, Throwable)
	 */
	@Override
	public void handleException(RepeatContext context, Throwable throwable) throws Throwable {
		for (ExceptionHandler handler : handlers) {
			handler.handleException(context, throwable);
		}
	}

}
