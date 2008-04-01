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
 * Simple base class for {@link BackOffPolicy} implementations that maintain no
 * state across invocations.
 * 
 * @author Rob Harrop
 * @author Dave Syer
 */
public abstract class StatelessBackOffPolicy implements BackOffPolicy {

	/**
	 * Delegates directly to the {@link #doBackOff()} method without passing on
	 * the {@link BackOffContext} argument which is not needed for stateless
	 * implementations.
	 */
	public final void backOff(BackOffContext backOffContext) throws BackOffInterruptedException {
		doBackOff();
	}

	/**
	 * Returns '<code>null</code>'. Subclasses can add behaviour, e.g.
	 * initial sleep before first attempt.
	 */
	public BackOffContext start(RetryContext status) {
		return null;
	}

	/**
	 * Sub-classes should implement this method to perform the actual back off.
	 */
	protected abstract void doBackOff() throws BackOffInterruptedException;
}
