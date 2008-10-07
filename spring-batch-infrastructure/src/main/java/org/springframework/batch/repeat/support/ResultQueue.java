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

import org.springframework.core.task.TaskExecutor;

/**
 * Abstraction for queue of {@link ResultHolder} objects. Acts a bit likeT a
 * {@link BlockingQueue} with the ability to count the number of items it
 * expects to ever hold. When clients schedule an item to be added they call
 * {@link #expect()}, and then collect the result later with {@link #take()}.
 * Result providers in another thread call {@link #put(Object)} to notify the
 * expecting client of a new result.
 * 
 * @author Dave Syer
 * @author Ben Hale
 */
interface ResultQueue<T> {

	/**
	 * In a master-slave pattern, the master calls this method paired with
	 * {@link #take()} to manage the flow of items. Normally a task is submitted
	 * for processing in another thread, at which point the master uses this
	 * method to keep track of the number of expected results. It has the
	 * personality of an counter increment, rather than a work queue, which is
	 * usually managed elsewhere, e.g. by a {@link TaskExecutor}.<br/><br/>
	 * Implementations may choose to block here, if they need to limit the
	 * number or rate of tasks being submitted.
	 * 
	 * @throws InterruptedException if the call blocks and is then interrupted.
	 */
	void expect() throws InterruptedException;

	/**
	 * Once it is expecting a result, clients call this method to satisfy the
	 * expectation. In a master-worker pattern, the workers call this method to
	 * deposit the result of a finished task on the queue for collection.
	 * 
	 * @param result the result for later collection.
	 * 
	 * @throws IllegalArgumentException if the queue is not expecting a new
	 * result
	 */
	void put(T result) throws IllegalArgumentException;

	/**
	 * Gets the next available result, blocking if there are none yet available.
	 * 
	 * @return a result previously deposited
	 * 
	 * @throws NoSuchElementException if there is no result expected
	 * @throws InterruptedException if the operation is interrupted while
	 * waiting
	 */
	T take() throws NoSuchElementException, InterruptedException;

	/**
	 * Used by master thread to verify that there are results available from
	 * {@link #take()} without possibly having to block and wait.
	 * 
	 * @return true if there are no results available
	 */
	boolean isEmpty();

	/**
	 * Check if any results are expected. Usually used by master thread to drain
	 * queue when it is finished.
	 * 
	 * @return true if more results are expected, but possibly not yet
	 * available.
	 */
	public boolean isExpecting();

}
