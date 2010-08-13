/*
 * Copyright 2006-2010 the original author or authors.
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
package org.springframework.batch.poller;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link Poller} that uses the callers thread to poll for a result as soon as
 * it is asked for. This is often appropriate if you expect a result relatively
 * quickly, or if there is only one such result expected (otherwise it is more
 * efficient to use a background thread to do the polling).
 * 
 * @author Dave Syer
 * 
 * @param <S> the type of the result
 */
public class DirectPoller<S> implements Poller<S> {

	private final long interval;

	public DirectPoller(long interval) {
		this.interval = interval;
	}

	/**
	 * Get a future for a non-null result from the callback. Only when the
	 * result is asked for (using {@link Future#get()} or
	 * {@link Future#get(long, TimeUnit)} will the polling actually start.
	 * 
	 * @see Poller#poll(Callable)
	 */
	public Future<S> poll(Callable<S> callable) throws Exception {
		return new DirectPollingFuture<S>(interval, callable);
	}

	private static class DirectPollingFuture<S> implements Future<S> {

		private final long startTime = System.currentTimeMillis();

		private volatile boolean cancelled;

		private volatile S result = null;

		private final long interval;

		private final Callable<S> callable;

		public DirectPollingFuture(long interval, Callable<S> callable) {
			this.interval = interval;
			this.callable = callable;
		}

		public boolean cancel(boolean mayInterruptIfRunning) {
			cancelled = true;
			return true;
		}

		public S get() throws InterruptedException, ExecutionException {
			try {
				return get(-1, TimeUnit.MILLISECONDS);
			}
			catch (TimeoutException e) {
				throw new IllegalStateException("Unexpected timeout waiting for result", e);
			}
		}

		public S get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {

			try {
				result = callable.call();
			}
			catch (Exception e) {
				throw new ExecutionException(e);
			}

			Long nextExecutionTime = startTime + interval;
			long currentTimeMillis = System.currentTimeMillis();
			long timeoutMillis = TimeUnit.MILLISECONDS.convert(timeout, unit);

			while (result == null && !cancelled) {

				long delta = nextExecutionTime - startTime;
				if (delta >= timeoutMillis && timeoutMillis > 0) {
					throw new TimeoutException("Timed out waiting for task to return non-null result");
				}

				if (nextExecutionTime > currentTimeMillis) {
					Thread.sleep(nextExecutionTime - currentTimeMillis);
				}

				currentTimeMillis = System.currentTimeMillis();
				nextExecutionTime = currentTimeMillis + interval;

				try {
					result = callable.call();
				}
				catch (Exception e) {
					throw new ExecutionException(e);
				}

			}

			return result;

		}

		public boolean isCancelled() {
			return cancelled;
		}

		public boolean isDone() {
			return cancelled || result != null;
		}

	}

}
