/*
 * Copyright 2006-2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Calls a collection of {@link ItemWriter}s in fixed-order sequence.<br>
 * <br>
 *
 * The implementation is thread-safe if all delegates are thread-safe.
 *
 * @author Robert Kasanicky
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @author Elimelec Burghelea
 */
public class CompositeItemWriter<T> implements ItemStreamWriter<T>, InitializingBean {

	private @Nullable List<ItemWriter<? super T>> delegates;

	private boolean ignoreItemStream = false;

	/**
	 * Default constructor
	 */
	public CompositeItemWriter() {

	}

	/**
	 * Convenience constructor for setting the delegates.
	 * @param delegates the list of delegates to use.
	 */
	public CompositeItemWriter(List<ItemWriter<? super T>> delegates) {
		setDelegates(delegates);
	}

	/**
	 * Convenience constructor for setting the delegates.
	 * @param delegates the array of delegates to use.
	 */
	@SafeVarargs
	public CompositeItemWriter(ItemWriter<? super T>... delegates) {
		this(Arrays.asList(delegates));
	}

	/**
	 * Establishes the policy whether to call the open, close, or update methods for the
	 * item writer delegates associated with the CompositeItemWriter.
	 * @param ignoreItemStream if false the delegates' open, close, or update methods will
	 * be called when the corresponding methods on the CompositeItemWriter are called. If
	 * true the delegates' open, close, nor update methods will not be called (default is
	 * false).
	 */
	public void setIgnoreItemStream(boolean ignoreItemStream) {
		this.ignoreItemStream = ignoreItemStream;
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	public void write(Chunk<? extends T> chunk) throws Exception {
		for (ItemWriter<? super T> writer : delegates) {
			writer.write(chunk);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(delegates != null, "The 'delegates' may not be null");
		Assert.state(!delegates.isEmpty(), "The 'delegates' may not be empty");
	}

	/**
	 * The list of item writers to use as delegates. Items are written to each of the
	 * delegates.
	 * @param delegates the list of delegates to use. The delegates list must not be null
	 * nor be empty.
	 */
	public void setDelegates(List<ItemWriter<? super T>> delegates) {
		this.delegates = delegates;
	}

	/**
	 * Close all delegates.
	 * @throws ItemStreamException thrown if one of the delegates fails to close. Original
	 * exceptions thrown by delegates are added as suppressed exceptions into this one, in
	 * the same order as delegates were registered.
	 */
	@SuppressWarnings("DataFlowIssue")
	@Override
	public void close() throws ItemStreamException {
		List<Exception> exceptions = new ArrayList<>();

		for (ItemWriter<? super T> writer : delegates) {
			if (!ignoreItemStream && (writer instanceof ItemStream itemStream)) {
				try {
					itemStream.close();
				}
				catch (Exception e) {
					exceptions.add(e);
				}
			}
		}

		if (!exceptions.isEmpty()) {
			String message = String.format("Failed to close %d delegate(s) due to exceptions", exceptions.size());
			ItemStreamException holder = new ItemStreamException(message);
			exceptions.forEach(holder::addSuppressed);
			throw holder;
		}
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		for (ItemWriter<? super T> writer : delegates) {
			if (!ignoreItemStream && (writer instanceof ItemStream itemStream)) {
				itemStream.open(executionContext);
			}
		}
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		for (ItemWriter<? super T> writer : delegates) {
			if (!ignoreItemStream && (writer instanceof ItemStream itemStream)) {
				itemStream.update(executionContext);
			}
		}
	}

}
