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
import org.springframework.batch.item.ItemProvider;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.exception.ExhaustedRetryException;
import org.springframework.batch.retry.exception.RetryException;
import org.springframework.batch.retry.policy.ItemProviderRetryPolicy;

/**
 * A {@link RetryCallback} that knows about and caches the value from an
 * {@link ItemProvider}. Used by the {@link ItemProviderRetryPolicy} to enable
 * external retry of the item processing.
 * 
 * @author Dave Syer
 * 
 * @see ItemProviderRetryPolicy
 * @see RetryPolicy#handleRetryExhausted(RetryContext)
 * 
 */
public class ItemProviderRetryCallback implements RetryCallback {

	private final static Log logger = LogFactory.getLog(ItemProviderRetryCallback.class);

	public static final String ITEM = ItemProviderRetryCallback.class + ".ITEM";

	private ItemProvider provider;

	private ItemProcessor processor;

	public ItemProviderRetryCallback(ItemProvider provider, ItemProcessor processor) {
		super();
		this.provider = provider;
		this.processor = processor;
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
				item = provider.next();
			}
			catch (Exception e) {
				throw new ExhaustedRetryException("Unexpected end of item provider", e);
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
	 * Accessor for the {@link ItemProvider}.
	 * @return the provider.
	 */
	public ItemProvider getProvider() {
		return provider;
	}

}
