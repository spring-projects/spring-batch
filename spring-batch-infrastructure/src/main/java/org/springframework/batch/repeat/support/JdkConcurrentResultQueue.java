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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import org.springframework.batch.repeat.RepeatException;

/**
 * An implementation of the {@link ResultQueue} that uses the Java 5 Concurrent Utilities.
 * 
 * @author Ben Hale
 */
class JdkConcurrentResultQueue extends RepeatInternalStateSupport implements ResultQueue {

	// Accumulation of result objects as they finish.
	private final BlockingQueue<ResultHolder> results;

	// Accumulation of dummy objects flagging expected results in the future.
	private final Semaphore waits;

	private final Object lock = new Object();

	private volatile int count = 0;

	JdkConcurrentResultQueue(int throttleLimit) {
		results = new LinkedBlockingQueue<ResultHolder>();
		waits = new Semaphore(throttleLimit);
	}

	protected void addResult(ResultHolder resultHolder) {
		results.add(resultHolder);
	}

	protected void aquireWait() throws InterruptedException {
		waits.acquire();
	}

	protected void releaseWait() {
		waits.release();
	}

	protected ResultHolder takeResult() throws InterruptedException {
		return (ResultHolder) results.take();
	}

	public boolean isEmpty() {
		return results.isEmpty();
	}

	public boolean isExpecting() {
		synchronized (lock) {
			// Base the decision about whether we expect more results on a
			// counter of the number of expected results actually collected.
			return count > 0;
		}
	}

	public void expect() {
		try {
			synchronized (lock) {
				aquireWait();
				count++;
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RepeatException("InterruptedException waiting for to acquire lock on input.");
		}
	}

	public void put(ResultHolder holder) {
		// There should be no need to block here, or to use offer()
		addResult(holder);
		// Take from the waits queue now to allow another result to
		// accumulate. But don't decrement the counter.
		releaseWait();
	}

	public ResultHolder take() {
		ResultHolder value;
		try {
			synchronized (lock) {
				value = takeResult();
				// Decrement the counter only when the result is collected.
				count--;
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RepeatException("InterruptedException while waiting for result.");
		}
		return value;
	}

}
