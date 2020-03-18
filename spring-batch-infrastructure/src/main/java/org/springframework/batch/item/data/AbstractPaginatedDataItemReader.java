/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.batch.item.data;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Iterator;

/**
 * A base class that handles basic reading logic based on the paginated
 * semantics of Spring Data's paginated facilities.  It also handles the
 * semantics required for restartability based on those facilities.
 * 
 * @author Michael Minella
 * @author Glenn Renfro
 * @since 2.2
 * @param <T> Type of item to be read
 */
public abstract class AbstractPaginatedDataItemReader<T> extends
AbstractItemCountingItemStreamItemReader<T> {

	protected volatile int page = 0;

	protected int pageSize = 10;

	protected Iterator<T> results;

	private Object lock = new Object();

	/**
	 * The number of items to be read with each page.
	 * 
	 * @param pageSize the number of items.  pageSize must be greater than zero.
	 */
	public void setPageSize(int pageSize) {
		Assert.isTrue(pageSize > 0, "pageSize must be greater than zero");
		this.pageSize = pageSize;
	}

	@Nullable
	@Override
	protected T doRead() throws Exception {

		synchronized (lock) {
			if(results == null || !results.hasNext()) {

				results = doPageRead();

				page ++;

				if(results == null || !results.hasNext()) {
					return null;
				}
			}


			if(results.hasNext()) {
				return results.next();
			}
			else {
				return null;
			}
		}
	}

	/**
	 * Method this {@link ItemStreamReader} delegates to
	 * for the actual work of reading a page.  Each time
	 * this method is called, the resulting {@link Iterator}
	 * should contain the items read within the next page.
	 * <br><br>
	 * If the {@link Iterator} is empty or null when it is
	 * returned, this {@link ItemReader} will assume that the
	 * input has been exhausted.
	 * 
	 * @return an {@link Iterator} containing the items within a page.
	 */
	protected abstract Iterator<T> doPageRead();

	@Override
	protected void doOpen() throws Exception {
	}

	@Override
	protected void doClose() throws Exception {
	}

	@Override
	protected void jumpToItem(int itemLastIndex) throws Exception {
		synchronized (lock) {
			page = itemLastIndex / pageSize;
			int current = itemLastIndex % pageSize;

			Iterator<T> initialPage = doPageRead();

			for(; current >= 0; current--) {
				initialPage.next();
			}
		}
	}
}
