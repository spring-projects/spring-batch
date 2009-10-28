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

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;

import org.springframework.batch.repeat.RepeatStatus;

/**
 * An implementation of the {@link ResultQueue} that throttles the number of
 * expected results, limiting it to a maximum at any given time.
 * 
 * @author Dave Syer
 */
public class ResultHolderResultQueue implements ResultQueue<ResultHolder> {

	// Accumulation of result objects as they finish.
	private final BlockingQueue<ResultHolder> results;

	// Accumulation of dummy objects flagging expected results in the future.
	private final Semaphore waits;

	private final Object lock = new Object();

	private volatile int count = 0;

	/**
	 * @param throttleLimit the maximum number of results that can be expected
	 * at any given time.
	 */
	public ResultHolderResultQueue(int throttleLimit) {
		results = new PriorityBlockingQueue<ResultHolder>(throttleLimit, new ResultHolderComparator());
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
		// Do not synchronize! Otherwise put and expect can deadlock.
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
		waits.acquire();
		// Don't acquire the lock in a synchronized block - might deadlock
		synchronized (lock) {
			count++;
		}
	}

	public void put(ResultHolder holder) throws IllegalArgumentException {
		if (!isExpecting()) {
			throw new IllegalArgumentException("Not expecting a result.  Call expect() before put().");
		}
		results.add(holder);
		// Take from the waits queue now to allow another result to
		// accumulate. But don't decrement the counter.
		waits.release();
		synchronized (lock) {
			lock.notifyAll();
		}
	}

	/**
	 * Get the next result as soon as it becomes available. <br/>
	 * <br/>
	 * Release result immediately if:
	 * <ul>
	 * <li>There is a result that is continuable.</li>
	 * </ul>
	 * Otherwise block if either:
	 * <ul>
	 * <li>There is no result (as per contract of {@link ResultQueue}).</li>
	 * <li>The number of results is less than the number expected.</li>
	 * </ul>
	 * Error if either:
	 * <ul>
	 * <li>Not expecting.</li>
	 * <li>Interrupted.</li>
	 * </ul>
	 * 
	 * @see ResultQueue#take()
	 */
	public ResultHolder take() throws NoSuchElementException, InterruptedException {
		if (!isExpecting()) {
			throw new NoSuchElementException("Not expecting a result.  Call expect() before take().");
		}
		ResultHolder value;
		synchronized (lock) {
			value = results.take();
			if (isContinuable(value)) {
				// Decrement the counter only when the result is collected.
				count--;
				return value;
			}
		}
		results.put(value);
		synchronized (lock) {
			while (count > results.size()) {
				lock.wait();
			}
			value = results.take();
			count--;
		}
		return value;
	}

	private boolean isContinuable(ResultHolder value) {
		return value.getResult() != null && value.getResult().isContinuable();
	}

	/**
	 * Compares ResultHolders so that one that is continuable ranks lowest.
	 * 
	 * @author Dave Syer
	 * 
	 */
	private static class ResultHolderComparator implements Comparator<ResultHolder> {
		public int compare(ResultHolder h1, ResultHolder h2) {
			RepeatStatus result1 = h1.getResult();
			RepeatStatus result2 = h2.getResult();
			if (result1 == null && result2 == null) {
				return 0;
			}
			if (result1 == null) {
				return -1;
			}
			else if (result2 == null) {
				return 1;
			}
			if ((result1.isContinuable() && result2.isContinuable())
					|| (!result1.isContinuable() && !result2.isContinuable())) {
				return 0;
			}
			if (result1.isContinuable()) {
				return -1;
			}
			return 1;
		}
	}

}
