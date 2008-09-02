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

import org.springframework.batch.retry.support.DefaultRetryState;

/**
 * Defines the basic set of operations implemented by {@link RetryOperations} to
 * execute operations with configurable retry behaviour.
 * 
 * @author Rob Harrop
 * @author Dave Syer
 */
public interface RetryOperations {

	/**
	 * Execute the supplied {@link RetryCallback} with the configured retry
	 * semantics. See implementations for configuration details.
	 * 
	 * @return the value returned by the {@link RetryCallback} upon successful
	 * invocation.
	 * @throws Exception any {@link Exception} raised by the
	 * {@link RetryCallback} upon unsuccessful retry.
	 */
	<T> T execute(RetryCallback<T> retryCallback) throws Exception;

	/**
	 * Execute the supplied {@link RetryCallback} with a fallback on exhausted
	 * retry to the {@link RecoveryCallback}. See implementations for
	 * configuration details.
	 * 
	 * @return the value returned by the {@link RetryCallback} upon successful
	 * invocation, and that returned by the {@link RecoveryCallback} otherwise.
	 * @throws Exception any {@link Exception} raised by the
	 * {@link RecoveryCallback} upon unsuccessful retry.
	 */
	<T> T execute(RetryCallback<T> retryCallback, RecoveryCallback<T> recoveryCallback) throws Exception;

	/**
	 * A simple stateful retry. Execute the supplied {@link RetryCallback} with
	 * a target object for the attempt identified by the {@link DefaultRetryState}.
	 * Exceptions thrown by the callback are always propagated immediately so
	 * the state is required to be able to identify the previous attempt, if
	 * there is one - hence the state is required. Normal patterns would see
	 * this method being used inside a transaction, where the callback might
	 * invalidate the transaction if it fails.<br/><br/>
	 * 
	 * See implementations for configuration details.
	 * 
	 * @return the value returned by the {@link RetryCallback} upon successful
	 * invocation, and that returned by the {@link RecoveryCallback} otherwise.
	 * @throws Exception any {@link Exception} raised by the
	 * {@link RecoveryCallback}.
	 * @throws ExhaustedRetryException if the last attempt for this state has
	 * already been reached
	 */
	<T> T execute(RetryCallback<T> retryCallback, RetryState retryState) throws Exception, ExhaustedRetryException;

	/**
	 * A stateful retry with a recovery path. Execute the supplied
	 * {@link RetryCallback} with a fallback on exhausted retry to the
	 * {@link RecoveryCallback} and a target object for the retry attempt
	 * identified by the {@link DefaultRetryState}.
	 * 
	 * @see #execute(RetryCallback, RetryState)
	 * 
	 * @return the value returned by the {@link RetryCallback} upon successful
	 * invocation, and that returned by the {@link RecoveryCallback} otherwise.
	 * @throws Exception any {@link Exception} raised by the
	 * {@link RecoveryCallback} upon unsuccessful retry.
	 */
	<T> T execute(RetryCallback<T> retryCallback, RecoveryCallback<T> recoveryCallback, RetryState retryState)
			throws Exception;

}
