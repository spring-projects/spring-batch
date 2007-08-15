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

package org.springframework.batch.retry;

import org.springframework.batch.retry.exception.TerminatedRetryException;

/**
 * Interface for interceptor that can be used to add behaviour to a retry.
 * Implementations of {@link RetryOperations} can chose to issue callbacks to an
 * interceptor during the retry lifecycle.
 * 
 * @author Dave Syer
 * 
 */
public interface RetryInterceptor {

	/**
	 * Called before the first attempt in a retry. For instance, implementers
	 * can set up state that is needed by the policies in the
	 * {@link RetryOperations}. The whole retry can be vetoed by returning
	 * false from this method, in which case a {@link TerminatedRetryException}
	 * will be thrown.
	 * 
	 * @param context the current {@link RetryContext}.
	 * @param callback the current {@link RetryCallback}.
	 * @return true if the retry should proceed.
	 */
	boolean open(RetryContext context, RetryCallback callback);

	/**
	 * Called after the final attempt (successful or not). Allow the interceptor
	 * to clean up any resource it is holding before control returns to the
	 * retry caller.
	 * 
	 * @param context the current {@link RetryContext}.
	 * @param callback the current {@link RetryCallback}.
	 * @param throwable the last exception that was thrown by the callback.
	 */
	void close(RetryContext context, RetryCallback callback, Throwable throwable);

	/**
	 * Called after every unsuccessful attempt at a retry.
	 * 
	 * @param context the current {@link RetryContext}.
	 * @param callback the current {@link RetryCallback}.
	 * @param throwable the last exception that was thrown by the callback.
	 */
	void onError(RetryContext context, RetryCallback callback, Throwable throwable);
}
