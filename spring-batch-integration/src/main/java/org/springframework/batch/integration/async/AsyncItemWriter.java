package org.springframework.batch.integration.async;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

public class AsyncItemWriter<T> implements ItemWriter<Future<T>>, InitializingBean {
	
	private ItemWriter<T> delegate;
	
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(delegate, "A delegate ItemWriter must be provided.");
	}
	
	/**
	 * @param delegate
	 */
	public void setDelegate(ItemWriter<T> delegate) {
		this.delegate = delegate;
	}

	public void write(List<? extends Future<T>> items) throws Exception {
		List<T> list = new ArrayList<T>();
		for (Future<T> future : items) {
			list.add(future.get());
		}
		delegate.write(list);
	}

}
