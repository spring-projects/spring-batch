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

package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.springframework.batch.classify.Classifier;
import org.springframework.batch.retry.ExhaustedRetryException;
import org.springframework.batch.retry.RecoveryCallback;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryListener;
import org.springframework.batch.retry.RetryOperations;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.RetryState;
import org.springframework.batch.retry.backoff.BackOffPolicy;
import org.springframework.batch.retry.context.RetryContextSupport;
import org.springframework.batch.retry.policy.RetryContextCache;
import org.springframework.batch.retry.support.DefaultRetryState;
import org.springframework.batch.retry.support.RetrySynchronizationManager;
import org.springframework.batch.retry.support.RetryTemplate;

/**
 * A special purpose retry template that deals specifically with multi-valued
 * stateful retry. This is useful in the case where the operation to be retried
 * operates on multiple items, and when it fails there is no way to decide which
 * (if any) of the items was responsible. The {@link RetryState} used in the
 * execute methods is composite, and when a failure occurs, all of the keys in
 * the composite are "tarred with the same brush". Subsequent attempts to
 * execute with any of the keys that have failed previously results in a new
 * attempt and the previous state is used to check the {@link RetryPolicy}. If
 * one of the failed items eventually succeeds then the others in the current
 * composite for that attempt will be cleared from the context cache (as
 * normal), but there may still be entries in the cache for the original failed
 * items. This might mean that an item that did not cause a failure is never
 * retried because other items in the same batch fail fatally first.
 * 
 * @author Dave Syer
 * 
 */
public class BatchRetryTemplate implements RetryOperations {

	private class BatchRetryState extends DefaultRetryState {

		private final Collection<RetryState> keys;

		public BatchRetryState(Collection<RetryState> keys) {
			super(keys);
			this.keys = new ArrayList<RetryState>(keys);
		}

	}

	private static class BatchRetryContext extends RetryContextSupport {

		private final Collection<RetryContext> contexts;

		public BatchRetryContext(RetryContext parent, Collection<RetryContext> contexts) {

			super(parent);

			this.contexts = contexts;
			int count = 0;

			for (RetryContext context : contexts) {
				int retryCount = context.getRetryCount();
				if (retryCount > count) {
					count = retryCount;
					registerThrowable(context.getLastThrowable());
				}
			}

		}

	}

	private static class InnerRetryTemplate extends RetryTemplate {

		@Override
		protected boolean canRetry(RetryPolicy retryPolicy, RetryContext context) {

			BatchRetryContext batchContext = (BatchRetryContext) context;

			for (RetryContext nextContext : batchContext.contexts) {
				if (!super.canRetry(retryPolicy, nextContext)) {
					return false;
				}
			}

			return true;

		}

		@Override
		protected RetryContext open(RetryPolicy retryPolicy, RetryState state) {

			BatchRetryState batchState = (BatchRetryState) state;

			Collection<RetryContext> contexts = new ArrayList<RetryContext>();
			for (RetryState retryState : batchState.keys) {
				contexts.add(super.open(retryPolicy, retryState));
			}

			return new BatchRetryContext(RetrySynchronizationManager.getContext(), contexts);

		}

		@Override
		protected void registerThrowable(RetryPolicy retryPolicy, RetryState state, RetryContext context, Throwable e) {

			BatchRetryState batchState = (BatchRetryState) state;
			BatchRetryContext batchContext = (BatchRetryContext) context;

			Iterator<RetryContext> contextIterator = batchContext.contexts.iterator();
			for (RetryState retryState : batchState.keys) {
				RetryContext nextContext = contextIterator.next();
				super.registerThrowable(retryPolicy, retryState, nextContext, e);
			}

		}

		@Override
		protected void close(RetryPolicy retryPolicy, RetryContext context, RetryState state, boolean succeeded) {

			BatchRetryState batchState = (BatchRetryState) state;
			BatchRetryContext batchContext = (BatchRetryContext) context;

			Iterator<RetryContext> contextIterator = batchContext.contexts.iterator();
			for (RetryState retryState : batchState.keys) {
				RetryContext nextContext = contextIterator.next();
				super.close(retryPolicy, nextContext, retryState, succeeded);
			}

		}

		@Override
		protected <T> T handleRetryExhausted(RecoveryCallback<T> recoveryCallback, RetryContext context,
				RetryState state) throws Exception {

			BatchRetryState batchState = (BatchRetryState) state;
			BatchRetryContext batchContext = (BatchRetryContext) context;

			// Accumulate exceptions to be thrown so all the keys get a crack
			Exception rethrowable = null;
			ExhaustedRetryException exhausted = null;

			Iterator<RetryContext> contextIterator = batchContext.contexts.iterator();
			for (RetryState retryState : batchState.keys) {

				RetryContext nextContext = contextIterator.next();

				try {
					super.handleRetryExhausted(null, nextContext, retryState);
				}
				catch (ExhaustedRetryException e) {
					exhausted = e;
				}
				catch (Exception e) {
					rethrowable = e;
				}

			}

			if (recoveryCallback != null) {
				return recoveryCallback.recover(context);
			}

			if (exhausted != null) {
				throw exhausted;
			}

			throw rethrowable;

		}

	}

	private final InnerRetryTemplate delegate = new InnerRetryTemplate();

	private final RetryTemplate regular = new RetryTemplate();

	private RetryPolicy retryPolicy;

	public <T> T execute(RetryCallback<T> retryCallback, Collection<RetryState> states) throws ExhaustedRetryException,
			Exception {
		RetryState batchState = new BatchRetryState(states);
		return delegate.execute(retryCallback, batchState);
	}

	public <T> T execute(RetryCallback<T> retryCallback, RecoveryCallback<T> recoveryCallback,
			Collection<RetryState> states) throws ExhaustedRetryException, Exception {
		RetryState batchState = new BatchRetryState(states);
		return delegate.execute(retryCallback, recoveryCallback, batchState);
	}
	
	public final <T> T execute(RetryCallback<T> retryCallback, RecoveryCallback<T> recoveryCallback,
			RetryState retryState) throws Exception, ExhaustedRetryException {
		return regular.execute(retryCallback, recoveryCallback, retryState);
	}

	public final <T> T execute(RetryCallback<T> retryCallback, RecoveryCallback<T> recoveryCallback) throws Exception {
		return regular.execute(retryCallback, recoveryCallback);
	}

	public final <T> T execute(RetryCallback<T> retryCallback, RetryState retryState) throws Exception,
			ExhaustedRetryException {
		return regular.execute(retryCallback, retryState);
	}

	public final <T> T execute(RetryCallback<T> retryCallback) throws Exception {
		return regular.execute(retryCallback);
	}

	public static List<RetryState> createState(List<?> keys) {
		List<RetryState> states = new ArrayList<RetryState>();
		for (Object key : keys) {
			states.add(new DefaultRetryState(key));
		}
		return states;
	}

	public static List<RetryState> createState(List<?> keys, Classifier<? super Throwable, Boolean> classifier) {
		List<RetryState> states = new ArrayList<RetryState>();
		for (Object key : keys) {
			states.add(new DefaultRetryState(key, classifier));
		}
		return states;
	}

	public void registerListener(RetryListener listener) {
		delegate.registerListener(listener);
		regular.registerListener(listener);
	}

	public void setBackOffPolicy(BackOffPolicy backOffPolicy) {
		delegate.setBackOffPolicy(backOffPolicy);
		regular.setBackOffPolicy(backOffPolicy);
	}

	public void setListeners(RetryListener[] listeners) {
		delegate.setListeners(listeners);
		regular.setListeners(listeners);
	}

	public void setRetryContextCache(RetryContextCache retryContextCache) {
		delegate.setRetryContextCache(retryContextCache);
		regular.setRetryContextCache(retryContextCache);
	}

	public void setRetryPolicy(RetryPolicy retryPolicy) {
		this.retryPolicy = retryPolicy;
		delegate.setRetryPolicy(retryPolicy);
		regular.setRetryPolicy(retryPolicy);
	}
	
	public boolean canRetry(RetryContext context) {
		return context==null ? true : retryPolicy.canRetry(context);
	}

}
