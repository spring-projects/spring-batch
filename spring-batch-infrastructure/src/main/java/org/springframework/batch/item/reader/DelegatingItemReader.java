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

package org.springframework.batch.item.reader;

import org.springframework.batch.io.Skippable;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.KeyedItemReader;
import org.springframework.batch.item.exception.StreamException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Simple wrapper around {@link ItemReader}. The input source is expected to
 * take care of open and close operations. If necessary it should be registered
 * as a step scoped bean to ensure that the lifecycle methods are called.
 * 
 * @author Dave Syer
 */
public class DelegatingItemReader extends AbstractItemReader implements Skippable, InitializingBean, ItemStream, KeyedItemReader {

	private ItemReader itemReader;

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(itemReader, "ItemReader must not be null.");
	}

	/**
	 * Get the next object from the input source.
	 * @throws Exception
	 * @see org.springframework.batch.item.ItemReader#read()
	 */
	public Object read() throws Exception {
		return itemReader.read();
	}

	/**
	 * @see ItemStream#update()
	 * @throws IllegalStateException if the parent template is not itself
	 * {@link ItemStream}.
	 */
	public void update() {
		if (itemReader instanceof ItemStream) {
			((ItemStream) itemReader).update();
		}
	}

	/**
	 * Setter for input source.
	 * @param source
	 */
	public void setItemReader(ItemReader source) {
		this.itemReader = source;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.batch.item.KeyedItemReader#getKey(java.lang.Object)
	 */
	public Object getKey(Object item) {
		return item;
	}

	public void skip() {
		if (itemReader instanceof Skippable) {
			((Skippable) itemReader).skip();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStream#open()
	 */
	public void open(ExecutionContext executionContext) throws StreamException {
		if (itemReader instanceof ItemStream) {
			((ItemStream) itemReader).open(executionContext);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStream#open()
	 */
	public void close() throws StreamException {
		if (itemReader instanceof ItemStream) {
			((ItemStream) itemReader).close();
		}
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
