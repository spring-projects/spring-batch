/*
 * Copyright 2026 the original author or authors.
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
package org.springframework.batch.infrastructure.item.data;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.util.Assert;

/**
 * A paging {@link ItemReader} that reads records by applying a query function to a
 * {@link Pageable}.
 * <p>
 * Performance of the reader is dependent on the query implementation, however setting a
 * reasonably large page size and matching that to the commit interval should yield better
 * performance.
 * <p>
 * The reader must be configured with a query function, {@linkplain Sort sort parameters},
 * and a pageSize greater than 0.
 * <p>
 * This implementation is thread-safe between calls to {@link #open(ExecutionContext)},
 * but remember to use {@code saveState=false} if used in a multi-threaded client (no
 * restart available).
 * <p>
 * It is important to note that this is a paging item reader and exceptions that are
 * thrown while reading the page itself (e.g., mapping results to objects in the
 * {@link PageableItemReader#doPageRead()}) will not be skippable since this reader has no
 * way of knowing if an exception should be skipped and therefore will continue to read
 * the same page until the skip limit is exceeded.
 * <p>
 * NOTE: The {@code PageableItemReader} only reads Java Objects, i.e., non primitives.
 *
 * @author Stefano Cordio
 * @since 6.1
 */
public class PageableItemReader<T> extends AbstractItemCountingItemStreamItemReader<T> {

	protected Log logger = LogFactory.getLog(getClass());

	private final int pageSize;

	private final Function<Pageable, ? extends Slice<? extends T>> query;

	private final Sort sort;

	private int page = 0;

	private int current = 0;

	private @Nullable List<? extends T> results;

	private final Lock lock = new ReentrantLock();

	/**
	 * Create a new {@link PageableItemReader}.
	 * @param pageSize the number of items to retrieve per page. Must be greater than 0.
	 * @param query a function that accepts a {@link Pageable} and returns a {@link Slice}
	 * of items
	 * @param sorts the sort parameters to use when building the {@link PageRequest}. Must
	 * not be empty. Use a {@link java.util.LinkedHashMap} for multiple entries to
	 * preserve sort order.
	 * @since 6.1
	 */
	public PageableItemReader(int pageSize, Function<Pageable, ? extends Slice<? extends T>> query,
			Map<String, Direction> sorts) {
		Assert.isTrue(pageSize > 0, "'pageSize' must be greater than 0");
		Assert.notNull(query, "'query' cannot be null");
		Assert.notEmpty(sorts, "'sorts' must not be empty");
		this.pageSize = pageSize;
		this.query = query;
		this.sort = Sort.by(sorts.entrySet().stream().map(PageableItemReader::createOrder).toList());
	}

	private static Order createOrder(Entry<String, Direction> entry) {
		return new Order(entry.getValue(), entry.getKey());
	}

	@Override
	protected @Nullable T doRead() throws Exception {

		lock.lock();
		try {
			boolean nextPageNeeded = (results != null && current >= results.size());

			if (results == null || nextPageNeeded) {

				if (logger.isDebugEnabled()) {
					logger.debug("Reading page " + page);
				}

				results = doPageRead();
				page++;

				if (results.isEmpty()) {
					return null;
				}

				if (nextPageNeeded) {
					current = 0;
				}
			}

			if (current < results.size()) {
				T item = results.get(current);
				current++;
				return item;
			}
			return null;
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Performs the actual reading of a page by applying the configured query. Available
	 * for overriding as needed.
	 * @return the list of items that make up the page
	 */
	protected List<? extends T> doPageRead() {
		PageRequest pageRequest = PageRequest.of(page, pageSize, sort);
		return query.apply(pageRequest).getContent();
	}

	@Override
	protected void doOpen() throws Exception {
	}

	@Override
	protected void doClose() throws Exception {
		lock.lock();
		try {
			current = 0;
			page = 0;
			results = null;
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	protected void jumpToItem(int itemLastIndex) throws Exception {
		lock.lock();
		try {
			page = itemLastIndex / pageSize;
			current = itemLastIndex % pageSize;
		}
		finally {
			lock.unlock();
		}
	}

}
