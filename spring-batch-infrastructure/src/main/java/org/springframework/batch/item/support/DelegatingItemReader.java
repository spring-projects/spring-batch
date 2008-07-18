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

package org.springframework.batch.item.support;

import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Simple wrapper around {@link ItemReader}. The item reader is expected to
 * take care of open and close operations. If necessary it should be registered
 * as a step scoped bean to ensure that the lifecycle methods are called.
 * 
 * The implementation is thread-safe if the delegate is thread-safe.
 * 
 * @author Dave Syer
 */
public class DelegatingItemReader<T> extends AbstractItemReader<T> implements InitializingBean {

	private ItemReader<T> itemReader;
	
	/**
	 * Default constructor.
	 */
	public DelegatingItemReader() {
		super();
	}

	/**
	 * Convenience constructor for setting mandatory property.
	 */
	public DelegatingItemReader(ItemReader<T> itemReader) {
		this();
		this.itemReader = itemReader;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(itemReader, "ItemReader must not be null.");
	}

	/**
	 * Get the next object from the input source.
	 * @throws Exception
	 * @see org.springframework.batch.item.ItemReader#read()
	 */
	public T read() throws Exception {
		return itemReader.read();
	}

	/**
	 * Setter for input source.
	 * @param source
	 */
	public void setItemReader(ItemReader<T> source) {
		this.itemReader = source;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStream#mark(org.springframework.batch.item.ExecutionContext)
	 */
	public void mark() {
		itemReader.mark();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStream#reset(org.springframework.batch.item.ExecutionContext)
	 */
	public void reset() {
		itemReader.reset();
	}
}
