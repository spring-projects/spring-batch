/*
 * Copyright 2013-present the original author or authors.
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

import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.util.Assert;

import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jspecify.annotations.Nullable;

/**
 * A base class that handles basic reading logic based on the paginated semantics of
 * Spring Data's paginated facilities. It also handles the semantics required for
 * restartability based on those facilities.
 * <p>
 * This reader is <b>not</b> thread-safe.
 *
 * @author Michael Minella
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 * @author Stefano Cordio
 * @since 2.2
 * @param <T> Type of item to be read
 */
public abstract class AbstractPaginatedDataItemReader<T> extends AbstractItemCountingItemStreamItemReader<T> {

	protected volatile int page = 0;

	protected int pageSize = 10;

	protected @Nullable Iterator<T> results;

	private final Lock lock = new ReentrantLock();

	/**
	 * The number of items to be read with each page.
	 * @param pageSize the number of items. pageSize must be greater than zero.
	 */
	public void setPageSize(int pageSize) {
		Assert.isTrue(pageSize > 0, "pageSize must be greater than zero");
		this.pageSize = pageSize;
	}

	@Override
	protected @Nullable T doRead() throws Exception {

		this.lock.lock();
		try {
			if (results == null || !results.hasNext()) {

				results = doPageRead();

				page++;

				if (!results.hasNext()) {
					return null;
				}
			}

			return results.next();
		}
		finally {
			this.lock.unlock();
		}
	}

	/**
	 * Method this {@link ItemStreamReader} delegates to for the actual work of reading a
	 * page. Each time this method is called, the resulting {@link Iterator} should
	 * contain the items read within the next page. <br>
	 * <br>
	 * If the {@link Iterator} is empty when it is returned, this {@link ItemReader} will
	 * assume that the input has been exhausted.
	 * @return an {@link Iterator} containing the items within a page.
	 */
	protected abstract Iterator<T> doPageRead();

	@Override
	protected void doOpen() throws Exception {
	}

	@Override
	protected void doClose() throws Exception {
		this.lock.lock();
		try {
			this.page = 0;
			this.results = null;
		}
		finally {
			this.lock.unlock();
		}
	}

	@Override
	protected void jumpToItem(int itemLastIndex) throws Exception {
		this.lock.lock();
		try {
			page = itemLastIndex / pageSize;
			int current = itemLastIndex % pageSize;

			Iterator<T> initialPage = doPageRead();

			for (; current > 0; current--) {
				initialPage.next();
			}
			this.results = initialPage;
		}
		finally {
			this.lock.unlock();
		}
	}

}
