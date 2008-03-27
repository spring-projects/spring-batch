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

import org.springframework.batch.item.FailedItemIdentifier;
import org.springframework.batch.item.ItemKeyGenerator;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemRecoverer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryException;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.policy.ItemWriterRetryPolicy;

/**
 * A {@link RetryCallback} that knows about and caches an item, and attempts to
 * process it using an {@link ItemWriter}. Used by the
 * {@link ItemWriterRetryPolicy} to enable external retry of the item
 * processing.
 * 
 * @author Dave Syer
 * 
 * @see ItemWriterRetryPolicy
 * @see RetryPolicy#handleRetryExhausted(RetryContext)
 * 
 */
public class ItemWriterRetryCallback implements RetryCallback {

	public static final String ITEM = ItemWriterRetryCallback.class.getName() + ".ITEM";

	private Object item;

	private ItemWriter writer;

	private ItemRecoverer recoverer;

	private ItemKeyGenerator keyGenerator;

	private FailedItemIdentifier failedItemIdentifier;

	private ItemKeyGenerator defaultKeyGenerator = new ItemKeyGenerator() {
		public Object getKey(Object item) {
			return item;
		}
	};

	public ItemWriterRetryCallback(Object item, ItemWriter writer) {
		this(item, writer, null);
	}

	public ItemWriterRetryCallback(Object item, ItemWriter writer, ItemKeyGenerator keyGenerator) {
		super();
		this.item = item;
		this.writer = writer;
		this.keyGenerator = keyGenerator;
	}

	/**
	 * Setter for injecting optional recovery handler. If it is not injected but
	 * the reader or writer implement {@link ItemRecoverer}, one of those will
	 * be used instead (preferring the reader to the writer if both would be
	 * appropriate).
	 * 
	 * @param recoveryHandler
	 */
	public void setRecoverer(ItemRecoverer recoverer) {
		this.recoverer = recoverer;
	}

	/**
	 * Public setter for the {@link ItemKeyGenerator}. If it is not injected
	 * but the reader or writer implement {@link ItemKeyGenerator}, one of
	 * those will be used instead (preferring the reader to the writer if both
	 * would be appropriate).
	 * @param keyGenerator the keyGenerator to set
	 */
	public void setKeyGenerator(ItemKeyGenerator keyGenerator) {
		this.keyGenerator = keyGenerator;
	}

	/**
	 * Public setter for the {@link FailedItemIdentifier}. If it is not
	 * injected but the reader or writer implement {@link FailedItemIdentifier},
	 * one of those will be used instead (preferring the reader to the writer if
	 * both would be appropriate).
	 * @param failedItemIdentifier the {@link FailedItemIdentifier} to set
	 */
	public void setFailedItemIdentifier(FailedItemIdentifier failedItemIdentifier) {
		this.failedItemIdentifier = failedItemIdentifier;
	}

	public Object doWithRetry(RetryContext context) throws Throwable {
		// This requires a collaboration with the RetryPolicy...
		if (!context.isExhaustedOnly()) {
			return process(context);
		}
		throw new RetryException("Recovery path requested in retry callback.");
	}

	public Object next(RetryContext context) {
		Object item = context.getAttribute(ITEM);
		if (item == null) {
			item = this.item;
			context.setAttribute(ITEM, item);
		}
		return item;
	}

	private Object process(RetryContext context) throws Exception {
		Object item = next(context);
		if (item != null) {
			writer.write(item);
		}
		context.removeAttribute(ITEM); // if successful
		return item;
	}

	/**
	 * Accessor for the {@link ItemRecoverer}. If the handler is null but the
	 * {@link ItemReader} is an instance of {@link ItemRecoverer}, then it will
	 * be returned instead. If none of those strategies works then a default
	 * implementation of {@link ItemKeyGenerator} will be used that just returns
	 * the item.
	 * 
	 * @return the {@link ItemRecoverer}.
	 */
	public ItemKeyGenerator getKeyGenerator() {
		if (keyGenerator != null) {
			return keyGenerator;
		}
		if (writer instanceof ItemKeyGenerator) {
			return (ItemKeyGenerator) writer;
		}
		return defaultKeyGenerator;
	}

	/**
	 * Accessor for the {@link FailedItemIdentifier}. If the handler is null
	 * but the {@link ItemReader} or {@link ItemWriter} is an instance of
	 * {@link FailedItemIdentifier}, then it will be returned instead. If none
	 * of those strategies works returns null.
	 * 
	 * @return the {@link FailedItemIdentifier}.
	 */
	public FailedItemIdentifier getFailedItemIdentifier() {
		if (failedItemIdentifier != null) {
			return failedItemIdentifier;
		}
		if (writer instanceof FailedItemIdentifier) {
			return (FailedItemIdentifier) writer;
		}
		return null;
	}

	/**
	 * Accessor for the {@link ItemRecoverer}. If the handler is null but the
	 * {@link ItemReader} is an instance of {@link ItemRecoverer}, then it will
	 * be returned instead.
	 * 
	 * @return the {@link ItemRecoverer}.
	 */
	public ItemRecoverer getRecoverer() {
		if (recoverer != null) {
			return recoverer;
		}
		if (writer instanceof ItemRecoverer) {
			return (ItemRecoverer) writer;
		}
		return null;
	}

}
