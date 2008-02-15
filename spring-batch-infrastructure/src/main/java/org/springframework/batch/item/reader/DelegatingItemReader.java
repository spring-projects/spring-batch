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
public class DelegatingItemReader extends AbstractItemReader implements Skippable, InitializingBean, ItemStream {

	private ItemReader inputSource;

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(inputSource, "ItemReader must not be null.");
	}

	/**
	 * Get the next object from the input source.
	 * @throws Exception
	 * @see org.springframework.batch.item.ItemReader#read()
	 */
	public Object read() throws Exception {
		return inputSource.read();
	}

	/**
	 * @see ItemStream#getExecutionContext()
	 * @throws IllegalStateException if the parent template is not itself
	 * {@link ItemStream}.
	 */
	public ExecutionContext getExecutionContext() {
		if (inputSource instanceof ItemStream) {
			return ((ItemStream) inputSource).getExecutionContext();
		}
		return new ExecutionContext();
	}

	/**
	 * @see ItemStream#restoreFrom(ExecutionContext)
	 * @throws IllegalStateException if the parent template is not itself
	 * {@link ItemStream}.
	 */
	public void restoreFrom(ExecutionContext data) {
		if (inputSource instanceof ItemStream) {
			((ItemStream) inputSource).restoreFrom(data);
		}
	}

	/**
	 * Setter for input source.
	 * @param source
	 */
	public void setItemReader(ItemReader source) {
		this.inputSource = source;
	}

	public ItemReader getItemReader() {
		return inputSource;
	}

	public void skip() {
		if (inputSource instanceof Skippable) {
			((Skippable) inputSource).skip();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStream#open()
	 */
	public void open() throws StreamException {
		if (inputSource instanceof ItemStream) {
			((ItemStream) inputSource).open();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStream#open()
	 */
	public void close() throws StreamException {
		if (inputSource instanceof ItemStream) {
			((ItemStream) inputSource).close();
		}
	}

	/**
	 * Delegates the call if the delegate is an {@link ItemStream}.
	 * 
	 * @see org.springframework.batch.item.ItemStream#isMarkSupported()
	 */
	public boolean isMarkSupported() {
		if (inputSource instanceof ItemStream) {
			return ((ItemStream) inputSource).isMarkSupported();
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStream#mark(org.springframework.batch.item.ExecutionContext)
	 */
	public void mark() {
		if (inputSource instanceof ItemStream) {
			((ItemStream) inputSource).mark();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStream#reset(org.springframework.batch.item.ExecutionContext)
	 */
	public void reset() {
		if (inputSource instanceof ItemStream) {
			((ItemStream) inputSource).reset();
		}
	}
}
