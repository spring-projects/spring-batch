/*
 * Copyright 2006-2019 the original author or authors.
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

import java.util.Map.Entry;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.PeekableItemReader;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.lang.Nullable;

/**
 * <p>
 * A {@link PeekableItemReader} that allows the user to peek one item ahead.
 * Repeated calls to {@link #peek()} will return the same item, and this will be
 * the next item returned from {@link #read()}.
 * </p>
 * 
 * <p>
 * Intentionally <b>not</b> thread-safe: it wouldn't be possible to honour the peek in
 * multiple threads because only one of the threads that peeked would get that
 * item in the next call to read.
 * </p>
 * 
 * @author Dave Syer
 * 
 */
public class SingleItemPeekableItemReader<T> implements ItemStreamReader<T>, PeekableItemReader<T> {

	private ItemReader<T> delegate;

	private T next;

	private ExecutionContext executionContext = new ExecutionContext();

	/**
	 * The item reader to use as a delegate. Items are read from the delegate
	 * and passed to the caller in {@link #read()}.
	 * 
	 * @param delegate the delegate to set
	 */
	public void setDelegate(ItemReader<T> delegate) {
		this.delegate = delegate;
	}

	/**
	 * Get the next item from the delegate (whether or not it has already been
	 * peeked at).
	 * 
	 * @see ItemReader#read()
	 */
	@Nullable
	@Override
	public T read() throws Exception, UnexpectedInputException, ParseException {
		if (next != null) {
			T item = next;
			next = null;
			// executionContext = new ExecutionContext();
			return item;
		}
		return delegate.read();
	}

	/**
	 * Peek at the next item, ensuring that if the delegate is an
	 * {@link ItemStream} the state is stored for the next call to
	 * {@link #update(ExecutionContext)}.
	 * 
	 * @return the next item (or null if there is none).
	 * 
	 * @see PeekableItemReader#peek()
	 */
	@Nullable
	@Override
	public T peek() throws Exception, UnexpectedInputException, ParseException {
		if (next == null) {
			updateDelegate(executionContext);
			next = delegate.read();
		}
		return next;
	}

	/**
	 * If the delegate is an {@link ItemStream}, just pass the call on,
	 * otherwise reset the peek cache.
	 * 
	 * @throws ItemStreamException if there is a problem
	 * @see ItemStream#close()
	 */
	@Override
	public void close() throws ItemStreamException {
		next = null;
		if (delegate instanceof ItemStream) {
			((ItemStream) delegate).close();
		}
		executionContext = new ExecutionContext();
	}

	/**
	 * If the delegate is an {@link ItemStream}, just pass the call on,
	 * otherwise reset the peek cache.
	 * 
	 * @param executionContext the current context
	 * @throws ItemStreamException if there is a problem
	 * @see ItemStream#open(ExecutionContext)
	 */
	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		next = null;
		if (delegate instanceof ItemStream) {
			((ItemStream) delegate).open(executionContext);
		}
		executionContext = new ExecutionContext();
	}

	/**
	 * If there is a cached peek, then retrieve the execution context state from
	 * that point. If there is no peek cached, then call directly to the
	 * delegate.
	 * 
	 * @param executionContext the current context
	 * @throws ItemStreamException if there is a problem
	 * @see ItemStream#update(ExecutionContext)
	 */
	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		if (next != null) {
			// Get the last state from the delegate instead of using
			// current value.
			for (Entry<String, Object> entry : this.executionContext.entrySet()) {
				executionContext.put(entry.getKey(), entry.getValue());
			}
			return;
		}
		updateDelegate(executionContext);
	}

	private void updateDelegate(ExecutionContext executionContext) {
		if (delegate instanceof ItemStream) {
			((ItemStream) delegate).update(executionContext);
		}
	}

}
