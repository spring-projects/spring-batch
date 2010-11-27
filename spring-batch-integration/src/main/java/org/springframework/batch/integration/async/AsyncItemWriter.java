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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

public class AsyncItemWriter<T> implements ItemWriter<Future<T>>, InitializingBean {
	
	private ItemWriter<T> delegate;
	
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(delegate, "A delegate ItemWriter must be provided.");
	}
	
	/**
	 * @param delegate
	 */
	public void setDelegate(ItemWriter<T> delegate) {
		this.delegate = delegate;
	}

	public void write(List<? extends Future<T>> items) throws Exception {
		List<T> list = new ArrayList<T>();
		for (Future<T> future : items) {
			list.add(future.get());
		}
		delegate.write(list);
	}

}
