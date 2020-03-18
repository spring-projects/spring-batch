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
package org.springframework.batch.item.database;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Abstract {@link org.springframework.batch.item.ItemStreamReader} for to extend when
 * reading database records in a paging fashion.
 *
 * <p>
 * Implementations should execute queries using paged requests of a size
 * specified in {@link #setPageSize(int)}. Additional pages are requested when
 * needed as {@link #read()} method is called, returning an object corresponding
 * to current position.
 * </p>
 *
 * @author Thomas Risberg
 * @author Dave Syer
 * @since 2.0
 */
public abstract class AbstractPagingItemReader<T> extends AbstractItemCountingItemStreamItemReader<T> 
        implements InitializingBean {

	protected Log logger = LogFactory.getLog(getClass());

	private volatile boolean initialized = false;

	private int pageSize = 10;

	private volatile int current = 0;

	private volatile int page = 0;

	protected volatile List<T> results;

	private Object lock = new Object();

	public AbstractPagingItemReader() {
		setName(ClassUtils.getShortName(AbstractPagingItemReader.class));
	}

	/**
	 * The current page number.
	 * @return the current page
	 */
	public int getPage() {
		return page;
	}

	/**
	 * The page size configured for this reader.
	 * @return the page size
	 */
	public int getPageSize() {
		return pageSize;
	}

	/**
	 * The number of rows to retrieve at a time.
	 *
	 * @param pageSize the number of rows to fetch per page
	 */
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	/**
	 * Check mandatory properties.
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.isTrue(pageSize > 0, "pageSize must be greater than zero");
	}

	@Nullable
	@Override
	protected T doRead() throws Exception {

		synchronized (lock) {

			if (results == null || current >= pageSize) {

				if (logger.isDebugEnabled()) {
					logger.debug("Reading page " + getPage());
				}

				doReadPage();
				page++;
				if (current >= pageSize) {
					current = 0;
				}

			}

			int next = current++;
			if (next < results.size()) {
				return results.get(next);
			}
			else {
				return null;
			}

		}

	}

	abstract protected void doReadPage();

	@Override
	protected void doOpen() throws Exception {

		Assert.state(!initialized, "Cannot open an already opened ItemReader, call close first");
		initialized = true;

	}

	@Override
	protected void doClose() throws Exception {

		synchronized (lock) {
			initialized = false;
			current = 0;
			page = 0;
			results = null;
		}

	}

	@Override
	protected void jumpToItem(int itemIndex) throws Exception {

		synchronized (lock) {
			page = itemIndex / pageSize;
			current = itemIndex % pageSize;
		}

		doJumpToPage(itemIndex);

		if (logger.isDebugEnabled()) {
			logger.debug("Jumping to page " + getPage() + " and index " + current);
		}

	}

	abstract protected void doJumpToPage(int itemIndex);

}
