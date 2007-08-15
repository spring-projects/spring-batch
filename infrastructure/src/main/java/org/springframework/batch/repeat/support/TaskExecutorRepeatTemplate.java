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

package org.springframework.batch.repeat.support;

import java.util.List;

import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.exception.RepeatException;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;

import edu.emory.mathcs.backport.java.util.concurrent.ArrayBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.BlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.Semaphore;

/**
 * Provides {@link RepeatOperations} support including interceptors that can be
 * used to modify or monitor the batch behaviour at run time.<br/>
 * 
 * This implementation is sufficient to be used to configure transactional
 * behaviour for each batch item by making the {@link RepeatCallback}
 * transactional, or for the whole batch by making the execute method
 * transactional (but only then if the task executor is synchronous).
 * Intermediate transactional chunks can be implemented using custom callbacks.<br/>
 * 
 * This class is thread safe if its collaborators are thread safe (interceptors,
 * terminationPolicy, callback). Normally this will be the case, but clients
 * need to be aware that if the task executor is asynchronous, then the other
 * collaborators should be also. In particular the {@link RepeatCallback} that
 * is wrapped in the execute method must be thread safe - often it is based on
 * some form of data source, which itself should be both thread safe and
 * transactional (multiple threads could be accessing it at any given time, and
 * each thread would have its own transaction).<br/>
 * 
 * @author Dave Syer
 * 
 */
public class TaskExecutorRepeatTemplate extends RepeatTemplate {

	/**
	 * Default limit for maximum number of concurrent unfinished results allowed
	 * by the template.
	 * {@link #getNextResult(RepeatContext, RepeatCallback, TerminationContext, List)}.
	 */
	public static final int DEFAULT_THROTTLE_LIMIT = 4;

	private int throttleLimit = DEFAULT_THROTTLE_LIMIT;

	private TaskExecutor taskExecutor = new SyncTaskExecutor();

	/**
	 * Setter for task executor to be used to run the individual item callbacks.
	 * 
	 * @param taskExecutor a TaskExecutor
	 * @throws IllegalArgumentException if the argument is null
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor);
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Use the {@link #taskExecutor} to generate a result. The internal state in
	 * this case is a queue of unfinished result holders of type
	 * {@link ResultHolder}. The holder with the return value should not be on
	 * the queue when this method exits. The queue is scoped in the calling
	 * method so there is no need to synchronize access.
	 * 
	 * @see org.springframework.batch.repeat.support.AbstracBatchemplate#getNextResult(org.springframework.batch.item.RepeatContext,
	 * org.springframework.batch.repeat.RepeatCallback,
	 * org.springframework.batch.TerminationContext, java.util.List)
	 */
	protected Object getNextResult(RepeatContext context, RepeatCallback callback, RepeatInternalState state) {

		ExecutingRunnable runnable = null;

		ResultQueue queue = (ResultQueue) state;

		do {

			/*
			 * Wrap the callback in a runnable that will add its result to the
			 * queue when it is ready.
			 */
			runnable = new ExecutingRunnable(callback, context, queue);

			/*
			 * Start the task possibly concurrently / in the future.
			 */
			taskExecutor.execute(runnable);

			/*
			 * Allow termination policy to update its state. This must happen
			 * immediately before or after the call to the task executor.
			 */
			update(context);

			/*
			 * Keep going until we get a result that is finished, or early
			 * termination...
			 */
		} while (queue.isEmpty() && !isComplete(context));

		Object result;
		try {
			result = queue.take().getResult();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			result = e;
		}
		return result;
	}

	/**
	 * Wait for all the results to appear on the queue and execute the after
	 * interceptors for each one.
	 * 
	 * @see org.springframework.batch.repeat.support.RepeatTemplate#waitForResults(org.springframework.batch.repeat.support.RepeatInternalState)
	 */
	protected boolean waitForResults(RepeatInternalState state) {

		ResultQueue futures = (ResultQueue) state;

		boolean result = true;

		while (futures.isExpecting()) {

			/*
			 * Careful that no runnables that are not going to finish ever get
			 * onto the queue, else this may block forever.
			 */
			ResultHolder future = (ResultHolder) futures.take();

			Object value;
			try {
				value = future.getResult();
			}
			catch (InterruptedException e) {
				// TODO: cancel batch?
				Thread.currentThread().interrupt();
				value = e;
			}
			if (value instanceof Throwable) {
				state.getThrowables().add(value);
			}

			executeAfterInterceptors(future.getContext(), value);
			result = result && canContinue(value);

		}

		Assert.state(futures.isEmpty(), "Future results should be empty at end of batch.");

		return result;
	}

	protected RepeatInternalState createInternalState(RepeatContext context) {
		// Queue of pending results:
		return new ResultQueue();
	}

	/**
	 * A runnable that puts its result on a queue when it is done.
	 * 
	 * @author Dave Syer
	 * 
	 */
	private static class ExecutingRunnable implements Runnable, ResultHolder {
		RepeatCallback callback;

		RepeatContext context;

		ResultQueue queue;

		Object result;

		public ExecutingRunnable(RepeatCallback callback, RepeatContext context, ResultQueue queue) {

			super();

			this.callback = callback;
			this.context = context;
			this.queue = queue;

			/*
			 * Tell the queue to expect a result.
			 */
			queue.expect();

		}

		/**
		 * Execute the batch callback, and store the result, or any exception
		 * that is thrown for retrieval later by caller.
		 * 
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			try {
				result = callback.doInIteration(context);
			}
			catch (Exception e) {
				result = e;
			}
			finally {
				queue.put(this);
			}
		}

		// TODO: Should we support cancellations?

		/**
		 * Get the result - never blocks because the queue manages waiting for
		 * the task to finish.
		 * 
		 * @throws InterruptedException if the thread is interrupted.
		 */
		public Object getResult() throws InterruptedException {
			return result;
		}

		/**
		 * Getter for the context.
		 */
		public RepeatContext getContext() {
			return this.context;
		}

	}

	/**
	 * Abstraction for queue of {@link ResultHolder} objects. Acts as a
	 * BlockingQueue with the ability to count the number of items it expects to
	 * ever hold. When clients schedule an item to be added they call
	 * {@link #expect()}, and then when the result is collected the queue is
	 * notified that it no longer expects another.
	 * 
	 * @author Dave Syer
	 * 
	 */
	public class ResultQueue extends RepeatInternalStateSupport {

		// Accumulation of result objects as they finish.
		private BlockingQueue results = new ArrayBlockingQueue(throttleLimit);

		// Accumulation of dummy objects flagging expected results in the
		// future.
		private Semaphore waits = new Semaphore(throttleLimit);

		// Arbitrary lock object.
		private Object lock = new Object();

		// Counter to monitor the difference between expected and actually
		// collected results. When this reaches zero there are really no more
		// results.
		private volatile int count = 0;

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
				waits.acquire();
				synchronized (lock) {
					count++;
				}
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RepeatException("InterruptedException waiting for to acquire lock on input.");
			}
		}

		public void put(ResultHolder holder) {
			// There should be no need to block here:
			results.add(holder);
			// Take from the waits queue now to allow another result to
			// accumulate. But don't decrement the counter.
			waits.release();
		}

		public ResultHolder take() {
			ResultHolder value;
			try {
				value = (ResultHolder) results.take();
				synchronized (lock) {
					// Decrement the counter only when the result is collected.
					count--;
				}
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RepeatException("Interrupted while waiting for result.");
			}
			return value;
		}

	}

	/**
	 * Interface for result holder. Should be implemented by subclasses so that
	 * the contract for
	 * {@link AbstracBatchemplate#getNextResult(RepeatContext, RepeatCallback, TerminationContext, List)}
	 * can be satisfied.
	 * 
	 * @author Dave Syer
	 */
	public interface ResultHolder {
		/**
		 * Get the result for client from this holder, blocking if necessary
		 * until it is ready.
		 * 
		 * @return the result.
		 * @throws InterruptedException if the thread is interrupted while
		 * waiting for the result.
		 * @throws IllegalStateException
		 */
		Object getResult() throws InterruptedException;

		/**
		 * Get the context in which the result evaluation is execututing.
		 * 
		 * @return the context of the result evaluation.
		 */
		RepeatContext getContext();
	}

	/**
	 * Public setter for the throttle limit. The throttle limit is the largest
	 * number of concurrent tasks that can be executing at one time - if a new
	 * task arrives and the throttle limit is breached we wait for one of the
	 * executing tasks to finish before submitting the new one to the
	 * {@link TaskExecutor}. Default value is {@value #DEFAULT_THROTTLE_LIMIT}.
	 * N.B. when used with a thread pooled {@link TaskExecutor} it doesn't make
	 * sense for the throttle limit to be less than the thread pool size.
	 * 
	 * @param throttleLimit the throttleLimit to set.
	 */
	public void setThrottleLimit(int throttleLimit) {
		this.throttleLimit = throttleLimit;
	}

}
