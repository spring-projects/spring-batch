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

package org.springframework.batch.retry.backoff;

import org.springframework.batch.retry.RetryContext;

/**
 * Strategy interface to control back off between attempts in a single
 * {@link org.springframework.batch.retry.support.RetryTemplate retry operation}.
 * <p/> Implementations are expected to be thread-safe and should be designed
 * for concurrent access. Configuration for each implementation is also expected
 * to be thread-safe but need not be suitable for high load concurrent access.
 * <p/> For each block of retry operations the {@link #start} method is called
 * and implementations can return an implementation-specific
 * {@link BackOffContext} that can be used to track state through subsequent
 * back off invocations. <p/> Each back off process is handled via a call to
 * {@link #backOff}. The
 * {@link org.springframework.batch.retry.support.RetryTemplate} will pass in
 * the corresponding {@link BackOffContext} object created by the call to
 * {@link #start}.
 * 
 * @author Rob Harrop
 * @author Dave Syer
 */
public interface BackOffPolicy {

	/**
	 * Start a new block of back off operations. Implementations can choose to
	 * pause when this method is called, but normally it returns immediately.
	 * 
	 * @param context the current retry context, which might contain information
	 * that we can use to decide how to proceed.
	 * @return the implementation-specific {@link BackOffContext} or '<code>null</code>'.
	 */
	BackOffContext start(RetryContext context);

	/**
	 * Back off/pause in an implementation-specific fashion. The passed in
	 * {@link BackOffContext} corresponds to the one created by the call to
	 * {@link #start} for a given retry operation set.
	 * 
	 * @throws BackOffInterruptedException if the attempt at back off is
	 * interrupted.
	 */
	void backOff(BackOffContext backOffContext) throws BackOffInterruptedException;

}
