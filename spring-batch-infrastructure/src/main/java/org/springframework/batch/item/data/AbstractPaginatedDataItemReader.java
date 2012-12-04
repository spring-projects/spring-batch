package org.springframework.batch.item.data;

import java.util.Iterator;

import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;

public abstract class AbstractPaginatedDataItemReader<T> extends
		AbstractItemCountingItemStreamItemReader<T> {

	protected volatile int page = 0;

	protected int pageSize = 10;

	protected Iterator<T> results;

	private Object lock = new Object();

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

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

	protected abstract Iterator doPageRead();

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

			Iterator initialPage = doPageRead();

			for(; current >= 0; current--) {
				initialPage.next();
			}
		}
	}
}
