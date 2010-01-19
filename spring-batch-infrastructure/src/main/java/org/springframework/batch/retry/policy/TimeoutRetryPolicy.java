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

package org.springframework.batch.retry.policy;

import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.context.RetryContextSupport;

/**
 * A {@link RetryPolicy} that allows a retry only if it hasn't timed out. The
 * clock is started on a call to {@link #open(RetryContext)}.
 * 
 * @author Dave Syer
 * 
 */
public class TimeoutRetryPolicy implements RetryPolicy {

	/**
	 * Default value for timeout (milliseconds).
	 */
	public static final long DEFAULT_TIMEOUT = 1000;

	private long timeout = DEFAULT_TIMEOUT;

	/**
	 * Setter for timeout in milliseconds. Default is {@link #DEFAULT_TIMEOUT}.
	 * @param timeout
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
	
	/**
	 * The value of the timeout.
	 * 
	 * @return the timeout in milliseconds
	 */
	public long getTimeout() {
		return timeout;
	}

	/**
	 * Only permits a retry if the timeout has not expired. Does not check the
	 * exception at all.
	 * 
	 * @see org.springframework.batch.retry.RetryPolicy#canRetry(org.springframework.batch.retry.RetryContext)
	 */
	public boolean canRetry(RetryContext context) {
		return ((TimeoutRetryContext) context).isAlive();
	}

	public void close(RetryContext context) {
	}

	public RetryContext open(RetryContext parent) {
		return new TimeoutRetryContext(parent, timeout);
	}

	public void registerThrowable(RetryContext context, Throwable throwable) {
		((RetryContextSupport) context).registerThrowable(throwable);
		// otherwise no-op - we only time out, otherwise retry everything...
	}

	private static class TimeoutRetryContext extends RetryContextSupport {
		private long timeout;

		private long start;

		public TimeoutRetryContext(RetryContext parent, long timeout) {
			super(parent);
			this.start = System.currentTimeMillis();
			this.timeout = timeout;
		}

		public boolean isAlive() {
			return (System.currentTimeMillis() - start) <= timeout;
		}
	}

}
