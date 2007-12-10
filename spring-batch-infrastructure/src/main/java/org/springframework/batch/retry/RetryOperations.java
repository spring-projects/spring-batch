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
	 * semantics.  See implementations for configuration details.
	 * 
	 * @return the value returned by the {@link RetryCallback} upon successful
	 * invocation.
	 * @throws Exception any {@link Exception} raised by the
	 * {@link RetryCallback} upon unsuccessful retry.
	 */
	Object execute(RetryCallback retryCallback) throws Exception;

}
