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
import org.springframework.batch.retry.exception.BackOffInterruptedException;
import org.springframework.util.ClassUtils;

/**
 * Implementation of {@link BackOffPolicy} that increases the back off period
 * for each retry attempt in a given set using the {@link Math#exp exponential}
 * function. <p/> This implementation is thread-safe and suitable for concurrent
 * access. Modifications to the configuration do not affect any retry sets that
 * are already in progress. <p/> The {@link #setExpSeed expSeed} property
 * controls the initial value passed to {@link Math#exp} and the
 * {@link #setIncrement increment} property controls by how much this value is
 * increased for each subsequent attempt.
 * 
 * @author Rob Harrop
 * @author Dave Syer
 * @since 2.1
 */
public class ExponentialBackOffPolicy implements BackOffPolicy {

	/**
	 * The default 'initialInterval' value - 100 millisecs. Coupled with the
	 * default 'multiplier' value this gives a useful initial spread of pauses
	 * for 1-5 retries.
	 */
	public static final long DEFAULT_INITIAL_INTERVAL = 100L;

	/**
	 * The default maximum backoff time (30 seconds).
	 */
	public static final long DEFAULT_MAX_INTERVAL = 30000L;

	/**
	 * The default 'multiplier' value - value 2 (100% increase per backoff).
	 */
	public static final double DEFAULT_MULTIPLIER = 2;

	/**
	 * The initial sleep interval.
	 */
	private volatile long initialInterval = DEFAULT_INITIAL_INTERVAL;

	/**
	 * The maximum value of the backoff period in milliseconds.
	 */
	private volatile long maxInterval = DEFAULT_MAX_INTERVAL;

	/**
	 * The value to increment the exp seed with for each retry attempt.
	 */
	private volatile double multiplier = DEFAULT_MULTIPLIER;

	/**
	 * Set the initial sleep interval value. Default is <code>1</code>
	 * millisecond. Cannot be set to a value less than one.
	 */
	public void setInitialInterval(long initialInterval) {
		this.initialInterval = (initialInterval > 1 ? initialInterval : 1);
	}

	/**
	 * Set the multiplier value. Default is '<code>1.0</code>'.
	 */
	public void setMultiplier(double multiplier) {
		this.multiplier = (multiplier > 1.0 ? multiplier : 1.0);
	}

	/**
	 * Setter for maximum back off period. Default is 30000 (30 seconds). the
	 * value will be reset to 1 if this method is called with a value less than
	 * 1.
	 * 
	 * @param maxInterval in milliseconds.
	 */
	public void setMaxInterval(long maxInterval) {
		this.maxInterval = maxInterval > 0 ? maxInterval : 1;
	}

	/**
	 * Returns a new instance of {@link ExponentialBackOffContext} configured
	 * with the 'expSeed' and 'increment' values.
	 */
	public BackOffContext start(RetryContext context) {
		return new ExponentialBackOffContext(this.initialInterval, this.multiplier, this.maxInterval);
	}

	/**
	 * Pause for a length of time equal to '<code>exp(backOffContext.expSeed)</code>'.
	 */
	public void backOff(BackOffContext backOffContext) throws BackOffInterruptedException {
		ExponentialBackOffContext context = (ExponentialBackOffContext) backOffContext;
		try {
			synchronized (context) {
				context.wait(context.getSleepAndIncrement());
			}
		}
		catch (InterruptedException e) {
			throw new BackOffInterruptedException("Thread interrupted while sleeping", e);
		}
	}

	private static class ExponentialBackOffContext implements BackOffContext {

		private final double multiplier;

		private long interval;

		private long maxInterval;

		public ExponentialBackOffContext(long expSeed, double multiplier, long maxInterval) {
			this.interval = expSeed;
			this.multiplier = multiplier;
			this.maxInterval = maxInterval;
		}

		public long getSleepAndIncrement() {
			long sleep = this.interval;
			if (sleep > maxInterval) {
				sleep = (long) maxInterval;
			}
			else {
				this.interval *= this.multiplier;
			}
			return sleep;
		}
	}

	public String toString() {
		return ClassUtils.getShortName(getClass()) + "[initialInterval=" + initialInterval + ", multiplier="
				+ multiplier + ", maxInterval=" + maxInterval + "]";
	}

}
