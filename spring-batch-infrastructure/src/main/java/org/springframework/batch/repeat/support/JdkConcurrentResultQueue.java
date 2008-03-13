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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * An implementation of the {@link ResultQueue} that uses the Java 5 Concurrent Utilities.
 * 
 * @author Ben Hale
 */
class JdkConcurrentResultQueue extends AbstractResultQueue implements RepeatInternalState {

	// Accumulation of result objects as they finish.
	private final BlockingQueue results;

	// Accumulation of dummy objects flagging expected results in the future.
	private final Semaphore waits;

	JdkConcurrentResultQueue(int throttleLimit) {
		results = new ArrayBlockingQueue(throttleLimit);
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

}
