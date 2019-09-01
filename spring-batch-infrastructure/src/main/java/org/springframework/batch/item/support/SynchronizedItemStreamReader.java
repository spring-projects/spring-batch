/*
 * Copyright 2015-2019 the original author or authors.
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
package org.springframework.batch.item.support;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 
 * This is a simple ItemStreamReader decorator with a synchronized ItemReader.read() 
 * method - which makes a non-thread-safe ItemReader thread-safe.
 * 
 * However, if reprocessing an item is problematic then using this will make a job not
 * restartable.
 * 
 * Here are some links about the motivation behind this class:
 * - https://projects.spring.io/spring-batch/faq.html#threading-reader}
 * - https://stackoverflow.com/a/20002493/2910265}
 * 
 * @author Matthew Ouyang
 * @since 3.0.4
 *
 * @param <T> type of object being read
 */
public class SynchronizedItemStreamReader<T> implements ItemStreamReader<T>, InitializingBean {

	private ItemStreamReader<T> delegate;

	public void setDelegate(ItemStreamReader<T> delegate) {
		this.delegate = delegate;
	}

	/**
	 * This delegates to the read method of the <code>delegate</code>
	 */
	@Nullable
	public synchronized T read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
		return this.delegate.read();
	}

	public void close() {
		this.delegate.close();
	}

	public void open(ExecutionContext executionContext) {
		this.delegate.open(executionContext);
	}

	public void update(ExecutionContext executionContext) {
		this.delegate.update(executionContext);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.delegate, "A delegate item reader is required");
	}
}
