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

package org.springframework.batch.repeat.exception;

import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatException;

/**
 * Default implementation of {@link ExceptionHandler} - just re-throws the first exception it encounters.
 * 
 * @author Dave Syer
 * 
 */
public class DefaultExceptionHandler implements ExceptionHandler {

	/**
	 * Rethrow the throwable in the collection's iterator. Wrap in a {@link RepeatException} if the first instance is
	 * not a {@link RuntimeException}.
	 * 
	 * @see org.springframework.batch.repeat.exception.ExceptionHandler#handleException(RepeatContext,
	 *      Throwable)
	 */
	public void handleException(RepeatContext context, Throwable throwable) throws RuntimeException {

		rethrow(throwable);

	}

	/**
	 * Convenience method to rethrow the Throwable instance. Wraps it in a {@link RepeatException} if it is not a
	 * {@link Exception}.
	 * 
	 * @param throwable a Throwable.
	 * @throws RuntimeException if the throwable is a {@link RuntimeException} just rethrow, otherwise wrap in a
	 *             {@link RepeatException}
	 */
	public static void rethrow(Throwable throwable) throws RuntimeException {
		if (throwable instanceof RuntimeException) {
			throw (RuntimeException) throwable;
		} else {
			throw new RepeatException("Exception in batch process", throwable);
		}
	}

}
