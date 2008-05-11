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

package org.springframework.batch.retry.callback;

import org.springframework.batch.item.ItemRecoverer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.retry.RecoveryCallback;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.policy.RecoveryCallbackRetryPolicy;

/**
 * A {@link RetryCallback} that knows about and caches an item, and attempts to
 * process it using an {@link ItemWriter}. Used by the
 * {@link RecoveryCallbackRetryPolicy} to enable external retry of the item
 * processing.
 * 
 * @author Dave Syer
 * 
 * @see RecoveryCallbackRetryPolicy
 * @see RetryPolicy#handleRetryExhausted(RetryContext)
 * 
 */
public class RecoveryRetryCallback implements RetryCallback {

	private final Object item;

	private final RetryCallback callback;

	private RecoveryCallback recoverer;

	private final Object key;

	private boolean forceRefresh = false;

	/**
	 * Constructor with mandatory properties. The key will be set to the item.
	 * 
	 * @param item the item to process
	 * @param writer the writer to use to process it
	 */
	public RecoveryRetryCallback(Object item, RetryCallback writer) {
		super();
		this.item = item;
		this.callback = writer;
		this.key = item;
	}

	/**
	 * Constructor with mandatory properties.
	 * 
	 * @param item the item to process
	 * @param writer the writer to use to process it
	 */
	public RecoveryRetryCallback(Object item, RetryCallback writer, Object key) {
		super();
		this.item = item;
		this.callback = writer;
		this.key = key;
	}

	/**
	 * Public getter for the key. This will be used to identify the item being
	 * processed, to see if it has previously failed.
	 * @return the key
	 */
	public Object getKey() {
		return key;
	}

	/**
	 * Setter for injecting optional recovery handler. If it is not injected but
	 * the reader or writer implement {@link ItemRecoverer}, one of those will
	 * be used instead (preferring the reader to the writer if both would be
	 * appropriate).
	 * 
	 * @param recoverer
	 */
	public void setRecoveryCallback(RecoveryCallback recoverer) {
		this.recoverer = recoverer;
	}

	/**
	 * Public setter for a flag signalling to clients of this callback that the
	 * processing is not a retry. It is always safe to leave this set to the
	 * default value (false), but in some cases it is possible to determine by
	 * examining the input data whether a failure has never been encountered
	 * (e.g. a message header saying that the message has never been consumed).
	 * Clients who have this information can avoid a cache query in such cases
	 * by setting the flag to true.
	 * 
	 * @param forceRefresh the flag value to set
	 */
	public void setForceRefresh(boolean forceRefresh) {
		this.forceRefresh = forceRefresh;
	}

	public Object doWithRetry(RetryContext context) throws Throwable {
		return callback.doWithRetry(context);
		// N.B. code used to check here for isExhaustedOnly and throw exception.
		// This is unnecessary because the callback could just throw the
		// exception itself if it wants to go to the recovery path.
	}

	public Object getItem() {
		return item;
	}

	public boolean isForceRefresh() {
		return forceRefresh;
	}

	/**
	 * Accessor for the {@link ItemRecoverer}.
	 * 
	 * @return the {@link ItemRecoverer}.
	 */
	public RecoveryCallback getRecoveryCallback() {
		return recoverer;
	}

}
