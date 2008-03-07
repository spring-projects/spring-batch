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


/**
 * Implementation of {@link BackOffPolicy} that pauses for a fixed period of
 * time before continuing. A pause is implemented using {@link Thread#sleep}.
 * <p/> {@link #backOff} is thread-safe and it is safe to call
 * {@link #setBackOffPeriod} during execution from multiple threads, however
 * this may cause a single retry operation to have pauses of different
 * intervals.
 * @author Rob Harrop
 * @since 2.1
 */
public class FixedBackOffPolicy extends StatelessBackOffPolicy {

	/**
	 * Default back off period - 1000ms.
	 */
	private static final long DEFAULT_BACK_OFF_PERIOD = 1000L;

	/**
	 * The back off period in milliseconds. Defaults to 1000ms.
	 */
	private volatile long backOffPeriod = DEFAULT_BACK_OFF_PERIOD;

	/**
	 * Set the back off period in milliseconds. Cannot be &lt; 1. Default value
	 * is 1000ms.
	 */
	public void setBackOffPeriod(long backOffPeriod) {
		this.backOffPeriod = (backOffPeriod > 0 ? backOffPeriod : 1);
	}

	/**
	 * Pause for the {@link #backOffPeriod} using {@link Thread#sleep}.
	 * @throws BackOffInterruptedException if interrupted during sleep.
	 */
	protected void doBackOff() throws BackOffInterruptedException {
		try {
			Object mutex = new Object();
			synchronized (mutex) {
				mutex.wait(this.backOffPeriod);
			}
		}
		catch (InterruptedException e) {
			throw new BackOffInterruptedException("Thread interrupted while sleeping", e);
		}
	}
}
