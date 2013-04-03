package org.springframework.batch.item.jms;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.util.Assert;

/**
 * 
 * @author George Foster
 * 
 * @param <T>
 */
public class CountingJmsItemReader<T> extends JmsItemReader<T> implements ItemStream {
	private static final String READ_COUNT = "read.count";

	private static final String READ_COUNT_MAX = "read.count.max";

	private final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();

	private final ReadLock readLock = reentrantReadWriteLock.readLock();

	private final WriteLock writeLock = reentrantReadWriteLock.writeLock();

	private long maxItemCount = Long.MAX_VALUE;

	private long currentItemCount = 0;

	@Override
	public T read() {
		T result = null;
		readLock.lock();
		try {
			if (currentItemCount < maxItemCount) {
				result = super.read();
				currentItemCount++;
			}
		}
		finally {
			readLock.unlock();
		}
		return result;
	}

	@Override
	public void open(final ExecutionContext executionContext) throws ItemStreamException {
		writeLock.lock();
		try {
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
		finally {
			writeLock.unlock();
		}
	}

	@Override
	public void update(final ExecutionContext executionContext) throws ItemStreamException {
		writeLock.lock();
		try {
			executionContext.putLong(READ_COUNT, currentItemCount);
			executionContext.putLong(READ_COUNT_MAX, maxItemCount);
		}
		finally {
			writeLock.unlock();
		}
	}

	@Override
	public void close() throws ItemStreamException {
		// no-op method
	}

	public void setMaxItemCount(final long maxItemCount) {
		Assert.isTrue(maxItemCount > 0, "maxItemCount should be greater than 0");
		this.maxItemCount = maxItemCount;
	}

}
