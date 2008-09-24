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

package org.springframework.batch.repeat.policy;

import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.context.RepeatContextSupport;

/**
 * Termination policy that times out after a fixed period. Allows graceful exit
 * from a batch if the latest result comes in after the timeout expires (i.e.
 * does not throw a timeout exception).<br/>
 * 
 * N.B. It may often be the case that the batch governed by this policy will be
 * transactional, and the transaction might have its own timeout. In this case
 * the transaction might throw a timeout exception on commit if its timeout
 * threshold is lower than the termination policy.
 * 
 * @author Dave Syer
 * 
 */
public class TimeoutTerminationPolicy extends CompletionPolicySupport {

	/**
	 * Default timeout value in millisecs (the value equivalent to 30 seconds).
	 */
	public static final long DEFAULT_TIMEOUT = 30000L;

	private long timeout = DEFAULT_TIMEOUT;

	/**
	 * Default constructor.
	 */
	public TimeoutTerminationPolicy() {
		super();
	}

	/**
	 * Construct a {@link TimeoutTerminationPolicy} with the specified timeout
	 * value (in milliseconds).
	 * 
	 * @param timeout
	 */
	public TimeoutTerminationPolicy(long timeout) {
		super();
		this.timeout = timeout;
	}

	/**
	 * Check the timeout and complete gracefully if it has expires.
	 * 
	 * @see org.springframework.batch.repeat.CompletionPolicy#isComplete(org.springframework.batch.repeat.RepeatContext)
	 */
	@Override
	public boolean isComplete(RepeatContext context) {
		return ((TimeoutBatchContext) context).isComplete();
	}

	/**
	 * Start the clock on the timeout.
	 * 
	 * @see org.springframework.batch.repeat.CompletionPolicy#start(RepeatContext)
	 */
	@Override
	public RepeatContext start(RepeatContext context) {
		return new TimeoutBatchContext(context);
	}

	protected class TimeoutBatchContext extends RepeatContextSupport {

		private volatile long time = System.currentTimeMillis();

		private final long timeout = TimeoutTerminationPolicy.this.timeout;

		public TimeoutBatchContext(RepeatContext context) {
			super(context);
		}

		public boolean isComplete() {
			return (System.currentTimeMillis() - time) > timeout;
		}

	}

}
