/*
 * Copyright 2006-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.integration.async;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

public class AsyncItemWriter<T> implements ItemStreamWriter<Future<T>>, InitializingBean {

	private static final Log logger = LogFactory.getLog(AsyncItemWriter.class);

	private ItemWriter<T> delegate;

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(delegate, "A delegate ItemWriter must be provided.");
	}

	/**
	 * @param delegate ItemWriter that does the actual writing of the Future results
	 */
	public void setDelegate(ItemWriter<T> delegate) {
		this.delegate = delegate;
	}

	/**
	 * In the processing of the {@link java.util.concurrent.Future}s passed, nulls are <em>not</em> passed to the
	 * delegate since they are considered filtered out by the {@link org.springframework.batch.integration.async.AsyncItemProcessor}'s
	 * delegated {@link org.springframework.batch.item.ItemProcessor}.  If the unwrapping
	 * of the {@link Future} results in an {@link ExecutionException}, that will be
	 * unwrapped and the cause will be thrown.
	 *
	 * @param items {@link java.util.concurrent.Future}s to be unwrapped and passed to the delegate
	 * @throws Exception The exception returned by the Future if one was thrown
	 */
	public void write(List<? extends Future<T>> items) throws Exception {
		List<T> list = new ArrayList<>();
		for (Future<T> future : items) {
			try {
				T item = future.get();

				if(item != null) {
					list.add(future.get());
				}
			}
			catch (ExecutionException e) {
				Throwable cause = e.getCause();

				if(cause != null && cause instanceof Exception) {
					logger.debug("An exception was thrown while processing an item", e);

					throw (Exception) cause;
				}
				else {
					throw e;
				}
			}
		}
		
		delegate.write(list);
	}

	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		if (delegate instanceof ItemStream) {
			((ItemStream) delegate).open(executionContext);
		}
	}

	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		if (delegate instanceof ItemStream) {
			((ItemStream) delegate).update(executionContext);
		}
	}

	@Override
	public void close() throws ItemStreamException {
		if (delegate instanceof ItemStream) {
			((ItemStream) delegate).close();
		}
	}
}
