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
package org.springframework.batch.integration.async;

import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class AsyncItemWriter<T> implements ItemWriter<Future<T>>, InitializingBean {
	
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
	 * delegated {@link org.springframework.batch.item.ItemProcessor}.
	 *
	 * @param items {@link java.util.concurrent.Future}s to be upwrapped and passed to the delegate
	 * @throws Exception
	 */
	public void write(List<? extends Future<T>> items) throws Exception {
		List<T> list = new ArrayList<T>();
		for (Future<T> future : items) {
			T item = future.get();

			if(item != null) {
				list.add(future.get());
			}
		}
		delegate.write(list);
	}
}
