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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemRecoverer;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.exception.ExhaustedRetryException;
import org.springframework.batch.retry.exception.RetryException;
import org.springframework.batch.retry.policy.ItemReaderRetryPolicy;

/**
 * A {@link RetryCallback} that knows about and caches the value from an
 * {@link ItemReader}. Used by the {@link ItemReaderRetryPolicy} to enable
 * external retry of the item processing.
 * 
 * @author Dave Syer
 * 
 * @see ItemReaderRetryPolicy
 * @see RetryPolicy#handleRetryExhausted(RetryContext)
 * 
 */
public class ItemReaderRetryCallback implements RetryCallback {

	private final static Log logger = LogFactory
			.getLog(ItemReaderRetryCallback.class);

	public static final String ITEM = ItemReaderRetryCallback.class.getName()
			+ ".ITEM";

	private ItemReader provider;

	private ItemProcessor processor;

	private ItemRecoverer recoverer;

	public ItemReaderRetryCallback(ItemReader provider,
			ItemProcessor processor) {
		super();
		this.provider = provider;
		this.processor = processor;
	}

	/**
	 * Setter for injecting optional recovery handler.
	 * 
	 * @param recoveryHandler
	 */
	public void setRecoverer(ItemRecoverer recoverer) {
		this.recoverer = recoverer;
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
			try {
				item = provider.read();
			} catch (Exception e) {
				throw new ExhaustedRetryException(
						"Unexpected end of item provider", e);
			}
			if (item == null) {
				// This is probably not fatal: in a batch we want to
				// exit gracefully...
				logger.info("ItemProvider exhausted during retry.");
			}
			context.setAttribute(ITEM, item);
		}
		return item;
	}

	private Object process(RetryContext context) throws Exception {
		Object item = next(context);
		if (item != null) {
			processor.process(item);
		}
		context.removeAttribute(ITEM); // if successful
		return item;
	}

	/**
	 * Accessor for the {@link ItemRecoverer}. If the handler is null but
	 * the {@link ItemReader} is an instanceof {@link ItemRecoverer},
	 * then it will be returned instead.
	 * 
	 * @return the {@link ItemRecoverer}.
	 */
	public ItemRecoverer getRecoverer() {
		if (recoverer != null) {
			return recoverer;
		}
		if (provider instanceof ItemRecoverer) {
			return (ItemRecoverer) provider;
		}
		return null;
	}

	/**
	 * Public getter for the {@link ItemReader}.
	 * 
	 * @return the {@link ItemReader} instance.
	 */
	public ItemReader getReader() {
		return provider;
	}

}
