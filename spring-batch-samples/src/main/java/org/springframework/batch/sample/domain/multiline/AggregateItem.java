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
package org.springframework.batch.sample.domain.multiline;

import org.springframework.batch.item.ItemReaderException;

/**
 * A wrapper type for an item that is used by {@link AggregateItemReader} to
 * identify the start and end of an aggregate record.
 * 
 * @see AggregateItemReader
 * 
 * @author Dave Syer
 * 
 */
public class AggregateItem<T> {

	@SuppressWarnings("unchecked")
	private static final AggregateItem FOOTER = new AggregateItem<Object>(false, true) {
		@Override
		public Object getItem() throws ItemReaderException {
			throw new IllegalStateException("Footer record has no item.");
		}
	};

	/**
	 * @param <T> the type of item nominally wrapped
	 * @return a static {@link AggregateItem} that is a footer.
	 */
	@SuppressWarnings("unchecked")
	public static final <T> AggregateItem<T> getFooter() {
		return FOOTER;
	}

	@SuppressWarnings("unchecked")
	private static final AggregateItem HEADER = new AggregateItem<Object>(true, false) {
		@Override
		public Object getItem() throws ItemReaderException {
			throw new IllegalStateException("Header record has no item.");
		}
	};

	/**
	 * @param <T> the type of item nominally wrapped
	 * @return a static {@link AggregateItem} that is a header.
	 */
	@SuppressWarnings("unchecked")
	public static final <T> AggregateItem<T> getHeader() {
		return HEADER;
	}

	private T item;

	private boolean footer = false;

	private boolean header = false;

	/**
	 * @param item
	 */
	public AggregateItem(T item) {
		super();
		this.item = item;
	}

	public AggregateItem(boolean header, boolean footer) {
		this(null);
		this.header = header;
		this.footer = footer;
	}

	/**
	 * Accessor for the wrapped item.
	 * 
	 * @return the wrapped item
	 * @throws IllegalStateException if called on a record for which either
	 * {@link #isHeader()} or {@link #isFooter()} answers true.
	 */
	public T getItem() throws IllegalStateException {
		return item;
	}

	/**
	 * Responds true if this record is a footer in an aggregate.
	 * @return true if this is the end of an aggregate record.
	 */
	public boolean isFooter() {
		return footer;
	}

	/**
	 * Responds true if this record is a header in an aggregate.
	 * @return true if this is the beginning of an aggregate record.
	 */
	public boolean isHeader() {
		return header;
	}

}
