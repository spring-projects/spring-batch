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

import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatOperations;
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
	protected ExitStatus getNextResult(RepeatContext context, RepeatCallback callback, RepeatInternalState state)
			throws Throwable {

		ExecutingRunnable runnable = null;

		ResultQueue queue = (ResultQueue) state;

		do {

			/*
			 * Wrap the callback in a runnable that will add its result to the
			 * queue when it is ready.
			 */
			runnable = new ExecutingRunnable(callback, context, queue);

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
		 * and there must be at least one because we just submitted one to teh
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

		ResultQueue queue = (ResultQueue) state;

		boolean result = true;

		while (queue.isExpecting()) {

			/*
			 * Careful that no runnables that are not going to finish ever get
			 * onto the queue, else this may block forever.
			 */
			ResultHolder future = (ResultHolder) queue.take();

			if (future.getError() != null) {
				state.getThrowables().add(future.getError());
			}
			else {
				ExitStatus status = future.getResult();
				result = result && canContinue(status);
				executeAfterInterceptors(future.getContext(), status);
			}

		}

		Assert.state(queue.isEmpty(), "Future results queue should be empty at end of batch.");

		return result;
	}

	protected RepeatInternalState createInternalState(RepeatContext context) {
		// Queue of pending results:
		return new ResultQueueFactory().getResultQueue(throttleLimit);
	}

	/**
	 * A runnable that puts its result on a queue when it is done.
	 * 
	 * @author Dave Syer
	 * 
	 */
	private static class ExecutingRunnable implements Runnable, ResultHolder {
		private RepeatCallback callback;

		private RepeatContext context;

		private ResultQueue queue;

		private ExitStatus result;

		private Throwable error;

		public ExecutingRunnable(RepeatCallback callback, RepeatContext context, ResultQueue queue) {

			super();

			this.callback = callback;
			this.context = context;
			this.queue = queue;

		}

		/**
		 * Tell the queue to expect a result.
		 */
		public void expect() {
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
				error = e;
			}
			finally {
				queue.put(this);
			}
		}

		/**
		 * Get the result - never blocks because the queue manages waiting for
		 * the task to finish.
		 */
		public ExitStatus getResult() {
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
