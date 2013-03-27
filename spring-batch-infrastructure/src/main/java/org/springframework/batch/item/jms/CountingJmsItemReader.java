package org.springframework.batch.item.jms;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;

/**
 * 
 * @author George Foster
 * 
 * @param <T>
 */
public class CountingJmsItemReader<T> extends JmsItemReader<T> implements ItemStream {
	private static final String READ_COUNT = "read.count";

	private static final String READ_COUNT_MAX = "read.count.max";

	private final Object mutex = new Object();

	private long maxItemCount = Long.MAX_VALUE;

	private long currentItemCount = 0;

	@Override
	public T read() {
		synchronized (mutex) {
			if (currentItemCount >= maxItemCount) {
				return null;
			}
			currentItemCount++;
		}
		return super.read();
	}

	@Override
	public void open(final ExecutionContext executionContext) throws ItemStreamException {
		synchronized (mutex) {
			Long readCountMax = executionContext.getLong(READ_COUNT_MAX);
			if (readCountMax != null) {
				maxItemCount = readCountMax;
			}

			// Current read count
			Long readCount = executionContext.getLong(READ_COUNT);
			if (readCount != null) {
				currentItemCount = readCount;
			}
		}
	}

	@Override
	public void update(final ExecutionContext executionContext) throws ItemStreamException {
		synchronized (mutex) {
			executionContext.putLong(READ_COUNT, currentItemCount);
			executionContext.putLong(READ_COUNT_MAX, maxItemCount);
		}
	}

	@Override
	public void close() throws ItemStreamException {
		// no-op method
	}

}
