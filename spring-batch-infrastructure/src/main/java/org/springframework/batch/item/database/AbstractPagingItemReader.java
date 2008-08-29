package org.springframework.batch.item.database;

import org.springframework.batch.item.support.AbstractItemReaderItemStream;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ClassUtils;
import org.springframework.util.Assert;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * Abstract {@link org.springframework.batch.item.ItemReader} for to extend when reading database records in a paging
 * fashion.
 *
 * Implementations should execute queries using paged requests of a size specified in {@link #setPageSize(int)}.
 * Additional pages are requested when needed as {@link #read()} method is called, returning an
 * object corresponding to current position.
 *
 * @author Thomas Risberg
 * @since 2.0
 */
public abstract class AbstractPagingItemReader<T> extends AbstractItemReaderItemStream<T> implements InitializingBean {

	protected Log logger = LogFactory.getLog(getClass());

	protected boolean initialized = false;

	protected int current = 0;

	protected int page = 0;

	protected int pageSize = 10;

	protected List<T> results;

	public AbstractPagingItemReader() {
		setName(ClassUtils.getShortName(AbstractPagingItemReader.class));
	}

	/**
	 * The number of rows to retreive at a time.
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
	public void afterPropertiesSet() throws Exception {
		Assert.isTrue(pageSize > 0, "pageSize must be greater than zero");
	}

	@Override
	protected T doRead() throws Exception {

		if (results == null || current >= pageSize) {

			if (logger.isDebugEnabled()) {
				logger.debug("Reading page " + page);
			}

			doReadPage();

			if (current >= pageSize) {
				current = 0;
			}
			page++;
		}

		if (current < results.size()) {
			return results.get(current++);
		}
		else {
			return null;
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

		initialized = false;

	}


	@Override
	protected void jumpToItem(int itemIndex) throws Exception {

		page = itemIndex / pageSize;
		current = itemIndex % pageSize;

		doJumpToPage(itemIndex);

		if (logger.isDebugEnabled()) {
			logger.debug("Jumping to page " + page + " and index " + current);
		}

	}

	abstract protected void doJumpToPage(int itemIndex);

}
