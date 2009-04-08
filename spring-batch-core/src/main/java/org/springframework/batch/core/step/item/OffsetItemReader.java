package org.springframework.batch.core.step.item;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

/**
 * Convenience wrapper for an ItemReader that keeps track of how many items
 * were successfully processed.
 */
class OffsetItemReader<T> implements ItemReader<T>, ItemStream {

	private static final String OFFSET_KEY = FaultTolerantStepFactoryBean.class.getName()+".OFFSET_KEY";
	private final ItemReader<? extends T> itemReader;
	private int offset;

	/**
	 * @param itemReader
	 */
	public OffsetItemReader(ItemReader<? extends T> itemReader) {
		this.itemReader = itemReader;
	}

	public T read() throws Exception, UnexpectedInputException, ParseException {
		for (int i=0; i<offset; i++) {
			// Discard items that are already processed 
			itemReader.read();
		}
		offset = 0;
		return itemReader.read();
	}

	/**
	 * {@inheritDoc}
	 */
	public void close() throws ItemStreamException {
	}

	/**
	 * {@inheritDoc}
	 */
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		offset = executionContext.getInt(OFFSET_KEY, 0);
	}

	/**
	 * {@inheritDoc}
	 */
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		 executionContext.putInt(OFFSET_KEY, offset);
	}

}