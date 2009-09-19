/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.batch.repeat.support;

import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * An implementation of the {@link ResultQueue} that throttles the number of
 * expected results, limiting it to a maximum at any given time.
 * 
 * @author Dave Syer
 */
public class ThrottleLimitResultQueue<T> implements ResultQueue<T> {

	// Accumulation of result objects as they finish.
	private final BlockingQueue<T> results;

	// Accumulation of dummy objects flagging expected results in the future.
	private final Semaphore waits;

	private final Object lock = new Object();

	private volatile int count = 0;

	/**
	 * @param throttleLimit the maximum number of results that can be expected
	 * at any given time.
	 */
	public ThrottleLimitResultQueue(int throttleLimit) {
		results = new LinkedBlockingQueue<T>();
		waits = new Semaphore(throttleLimit);
	}

	public boolean isEmpty() {
		return results.isEmpty();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.repeat.support.ResultQueue#isExpecting()
	 */
	public boolean isExpecting() {
		// Base the decision about whether we expect more results on a
		// counter of the number of expected results actually collected.
		// Do not synchronize!  Otherwise put and expect can deadlock.
		return count > 0;
	}

	/**
	 * Tell the queue to expect one more result. Blocks until a new result is
	 * available if already expecting too many (as determined by the throttle
	 * limit).
	 * 
	 * @see ResultQueue#expect()
	 */
	public void expect() throws InterruptedException {
		synchronized (lock) {
			waits.acquire();
			count++;
		}
	}

	public void put(T holder) throws IllegalArgumentException {
		if (!isExpecting()) {
			throw new IllegalArgumentException("Not expecting a result.  Call expect() before put().");
		}
		// There should be no need to block here, or to use offer()
		results.add(holder);
		// Take from the waits queue now to allow another result to
		// accumulate. But don't decrement the counter.
		waits.release();
	}

	public T take() throws NoSuchElementException, InterruptedException {
		if (!isExpecting()) {
			throw new NoSuchElementException("Not expecting a result.  Call expect() before take().");
		}
		T value;
		synchronized (lock) {
			value = results.take();
			// Decrement the counter only when the result is collected.
			count--;
		}
		return value;
	}

}
