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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatException;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;

/**
 * Provides {@link RepeatOperations} support including interceptors that can be
 * used to modify or monitor the behaviour at run time.<br/>
 * 
 * This implementation is sufficient to be used to configure transactional
 * behaviour for each item by making the {@link RepeatCallback} transactional,
 * or for the whole batch by making the execute method transactional (but only
 * then if the task executor is synchronous).<br/>
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
	 * {@link #getNextResult(RepeatContext, RepeatCallback, RepeatInternalState)}
	 * .
	 */
	public static final int DEFAULT_THROTTLE_LIMIT = 4;

	private int throttleLimit = DEFAULT_THROTTLE_LIMIT;

	private TaskExecutor taskExecutor = new SyncTaskExecutor();

	/**
	 * Public setter for the throttle limit. The throttle limit is the largest
	 * number of concurrent tasks that can be executing at one time - if a new
	 * task arrives and the throttle limit is breached we wait for one of the
	 * executing tasks to finish before submitting the new one to the
	 * {@link TaskExecutor}. Default value is {@link #DEFAULT_THROTTLE_LIMIT}.
	 * N.B. when used with a thread pooled {@link TaskExecutor} the thread pool
	 * might prevent the throttle limit actually being reached (so make the core
	 * pool size larger than the throttle limit if possible).
	 * 
	 * @param throttleLimit the throttleLimit to set.
	 */
	public void setThrottleLimit(int throttleLimit) {
		this.throttleLimit = throttleLimit;
	}

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
	 * Use the {@link #setTaskExecutor(TaskExecutor)} to generate a result. The
	 * internal state in this case is a queue of unfinished result holders of
	 * type {@link ResultHolder}. The holder with the return value should not be
	 * on the queue when this method exits. The queue is scoped in the calling
	 * method so there is no need to synchronize access.
	 * 
	 */
	protected RepeatStatus getNextResult(RepeatContext context, RepeatCallback callback, RepeatInternalState state)
			throws Throwable {

		ExecutingRunnable runnable = null;

		ResultQueue<ResultHolder> queue = ((ResultQueueInternalState) state).getResultQueue();
		ActivityBarrier lock = ((ResultQueueInternalState) state).getLock();

		do {

			/*
			 * Wrap the callback in a runnable that will add its result to the
			 * queue when it is ready.
			 */
			runnable = new ExecutingRunnable(callback, context, queue, lock);

			/**
			 * Tell the runnable that it can expect a result. This could have
			 * been in-lined with the constructor, but it might block, so it's
			 * better to do it here, since we have the option (it's a private
			 * class).
			 */
			runnable.expect();

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

		/*
		 * N.B. If the queue is empty then take() blocks until a result appears,
		 * and there must be at least one because we just submitted one to the
		 * task executor.
		 */
		ResultHolder result = queue.take();
		if (result.getError() != null) {
			throw result.getError();
		}
		return result.getResult();
	}

	/**
	 * Wait for all the results to appear on the queue and execute the after
	 * interceptors for each one.
	 * 
	 * @see org.springframework.batch.repeat.support.RepeatTemplate#waitForResults(org.springframework.batch.repeat.support.RepeatInternalState)
	 */
	protected boolean waitForResults(RepeatInternalState state) {

		ResultQueue<ResultHolder> queue = ((ResultQueueInternalState) state).getResultQueue();
		ActivityBarrier lock = ((ResultQueueInternalState) state).getLock();

		boolean result = true;

		while (queue.isExpecting()) {

			lock.release();

			/*
			 * Careful that no runnables that are not going to finish ever get
			 * onto the queue, else this may block forever.
			 */
			ResultHolder future;
			try {
				future = (ResultHolder) queue.take();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RepeatException("InterruptedException while waiting for result.");
			}

			if (future.getError() != null) {
				state.getThrowables().add(future.getError());
			}
			else {
				RepeatStatus status = future.getResult();
				result = result && canContinue(status);
				executeAfterInterceptors(future.getContext(), status);
			}

		}

		Assert.state(queue.isEmpty(), "Future results queue should be empty at end of batch.");

		return result;
	}

	protected RepeatInternalState createInternalState(RepeatContext context) {
		// Queue of pending results:
		return new ResultQueueInternalState(throttleLimit);
	}

	/**
	 * A runnable that puts its result on a queue when it is done.
	 * 
	 * @author Dave Syer
	 * 
	 */
	private class ExecutingRunnable implements Runnable, ResultHolder {

		private final RepeatCallback callback;

		private final RepeatContext context;

		private final ResultQueue<ResultHolder> queue;

		private volatile RepeatStatus result;

		private volatile Throwable error;

		private final ActivityBarrier lock;

		public ExecutingRunnable(RepeatCallback callback, RepeatContext context, ResultQueue<ResultHolder> queue,
				ActivityBarrier lock) {

			super();

			this.callback = callback;
			this.context = context;
			this.queue = queue;
			this.lock = lock;

		}

		/**
		 * Tell the queue to expect a result.
		 */
		public void expect() {
			try {
				queue.expect();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RepeatException("InterruptedException waiting for to acquire lock on input.");
			}
		}

		/**
		 * Execute the batch callback, and store the result, or any exception
		 * that is thrown for retrieval later by caller.
		 * 
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			boolean clearContext = false;
			try {
				if (RepeatSynchronizationManager.getContext() == null) {
					clearContext = true;
					RepeatSynchronizationManager.register(context);
				}

				lock.acquire();
				result = callback.doInIteration(context);

			}
			catch (Exception e) {
				error = e;
			}
			finally {

				lock.release(isComplete(context), result);

				if (clearContext) {
					RepeatSynchronizationManager.clear();
				}

				queue.put(this);

			}
		}

		/**
		 * Get the result - never blocks because the queue manages waiting for
		 * the task to finish.
		 */
		public RepeatStatus getResult() {
			return result;
		}

		/**
		 * Get the error - never blocks because the queue manages waiting for
		 * the task to finish.
		 */
		public Throwable getError() {
			return error;
		}

		/**
		 * Getter for the context.
		 */
		public RepeatContext getContext() {
			return this.context;
		}

	}

	/**
	 * @author Dave Syer
	 * 
	 */
	private static class ResultQueueInternalState extends RepeatInternalStateSupport {

		private final ResultQueue<ResultHolder> results;

		private final ActivityBarrier lock;

		/**
		 * @param throttleLimit the throttle limit for the result queue
		 */
		public ResultQueueInternalState(int throttleLimit) {
			super();
			this.results = new ThrottleLimitResultQueue<ResultHolder>(throttleLimit);
			this.lock = new ActivityBarrier();
		}

		/**
		 * @return the lock instance
		 */
		public ActivityBarrier getLock() {
			return lock;
		}

		/**
		 * @return the result queue
		 */
		public ResultQueue<ResultHolder> getResultQueue() {
			return results;
		}

	}

	/**
	 * <p>
	 * Encapsulates the locking concerns needed when waiting for concurrent
	 * repeat tasks to complete. Some tasks may return FINISHED before the rest
	 * have completed, and furthermore one or more of the rest might be
	 * CONTINUABLE or might have ended in an error and still have more work to
	 * do.
	 * </p>
	 * 
	 * <p>
	 * Uses wait and notify to co-ordinate the end of the repeat iterations, so
	 * that the process can end only when either the completion policy signals
	 * completion, or all workers have signalled that they are FINISHED (and had
	 * a chance to accept more work if they are initially CONTINUABLE).
	 * </p>
	 * 
	 * <p>
	 * The net effect of using this lock protocol is that all workers (up to the
	 * throttle limit) may perform no work for the last iteration and return
	 * FINISHED. Contrast this with the single threaded case where the only one
	 * call resulting in FINISHED is made to the repeat callback.
	 * </p>
	 * 
	 * @author Dave Syer
	 */
	private static class ActivityBarrier {

		private static Log logger = LogFactory.getLog(ActivityBarrier.class);

		private volatile int active = 0;

		private volatile boolean paused = false;

		private final Object lock = new Object();

		/**
		 * Atomic acquisition of resources needed to track concurrent execution.
		 * Call this method before every repeat callback execution.
		 */
		public void acquire() {
			synchronized (lock) {
				active++;
				paused = false;
			}
		}

		/**
		 * <p>
		 * Release the resources acquired in {@link #acquire()}. Call this
		 * method in a finally block after every repeat callback execution.
		 * </p>
		 * 
		 * @param complete true if we know from the completion policy that the
		 * iteration should end
		 * @param result the latest result from a callback
		 */
		public void release(boolean complete, RepeatStatus result) {

			boolean stillActive = false;

			synchronized (lock) {

				
				active--;
				stillActive = active > 0 || paused;

				if (logger.isDebugEnabled()) {
					logger.debug("Completed callback with result = " + result + ", " + active
							+ " active callbacks, and paused=" + paused);
				}

			}

			if (result == RepeatStatus.FINISHED) {
				if (stillActive) {
					logger.debug("Waiting for other active callbacks to finish.");
					synchronized (lock) {
						try {
							lock.wait();
						}
						catch (InterruptedException e) {
							logger.info("Interrupted waiting for active callbacks");
							Thread.currentThread().interrupt();
						}
					}
				}
				else {
					synchronized (lock) {
						logger.debug("Notifying other waiting callbacks on finish.");
						paused = false;
						lock.notifyAll();
					}
				}
			}
			else {
				if (complete) {
					synchronized (lock) {
						logger.debug("Notifying other waiting callbacks on policy based completion.");
						paused = false;
						lock.notifyAll();
					}
				} else {
					synchronized (lock) {						
						paused = true;
					}
				}
			}

		}

		/**
		 * Release all waiting workers unconditionally. Call this method when
		 * iteration has ended in case any workers are still waiting.
		 */
		public void release() {
			synchronized (lock) {
				lock.notifyAll();
			}
		}

	}

}
