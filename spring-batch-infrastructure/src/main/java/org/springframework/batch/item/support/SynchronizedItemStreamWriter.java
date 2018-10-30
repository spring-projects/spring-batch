package org.springframework.batch.item.support;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.util.List;

/**
 * An {@link ItemStreamWriter} decorator with a synchronized
 * {@link SynchronizedItemStreamWriter#write write()} method
 *
 * @author Dimitrios Liapis
 */
public class SynchronizedItemStreamWriter<T> implements ItemStreamWriter<T>, InitializingBean {

	private ItemStreamWriter<T> delegate;

	public void setDelegate(ItemStreamWriter<T> delegate) {
		this.delegate = delegate;
	}

	/**
	 * This delegates to the write method of the <code>delegate</code>
	 */
	@Override
	public synchronized void write(List<? extends T> items) throws Exception {
		this.delegate.write(items);
	}

	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		this.delegate.open(executionContext);
	}

	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		this.delegate.update(executionContext);
	}

	@Override
	public void close() throws ItemStreamException {
		this.delegate.close();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.delegate, "A delegate item reader is required");
	}
}
