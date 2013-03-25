package org.springframework.batch.item.jms;

import org.springframework.util.Assert;

/**
 * 
 * @author George Foster
 * 
 * @param <T>
 */
public class CountingJmsItemReader<T> extends JmsItemReader<T> {
	private long maxItems = Long.MAX_VALUE;

	private long numItemReads = 0;

	@Override
	public synchronized T read() {
		if (numItemReads >= maxItems) {
			return null;
		}

		numItemReads++;
		return super.read();
	}

	public void setMaxItems(final long maxItems) {
		Assert.isTrue(maxItems > 0);
		this.maxItems = maxItems;
	}
}
