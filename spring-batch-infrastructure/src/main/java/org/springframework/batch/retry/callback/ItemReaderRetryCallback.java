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
import org.springframework.batch.item.ItemKeyGenerator;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemRecoverer;
import org.springframework.batch.item.ItemWriter;
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

	private final static Log logger = LogFactory.getLog(ItemReaderRetryCallback.class);

	public static final String ITEM = ItemReaderRetryCallback.class.getName() + ".ITEM";

	private ItemReader reader;

	private ItemWriter writer;

	private ItemRecoverer recoverer;

	private ItemKeyGenerator keyGenerator;

	private ItemKeyGenerator defaultKeyGenerator = new ItemKeyGenerator() {
		public Object getKey(Object item) {
			return item;
		}
	};

	public ItemReaderRetryCallback(ItemReader reader, ItemWriter writer) {
		this(reader, null, writer);
	}

	public ItemReaderRetryCallback(ItemReader reader, ItemKeyGenerator keyGenerator, ItemWriter writer) {
		super();
		this.reader = reader;
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
				item = reader.read();
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
		if (reader instanceof ItemKeyGenerator) {
			return (ItemKeyGenerator) reader;
		}
		if (writer instanceof ItemKeyGenerator) {
			return (ItemKeyGenerator) writer;
		}
		return defaultKeyGenerator;
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
		if (reader instanceof ItemRecoverer) {
			return (ItemRecoverer) reader;
		}
		if (writer instanceof ItemRecoverer) {
			return (ItemRecoverer) writer;
		}
		return null;
	}

	/**
	 * Public getter for the {@link ItemReader}.
	 * 
	 * @return the {@link ItemReader} instance.
	 */
	public ItemReader getReader() {
		return reader;
	}

}
