package org.springframework.batch.item.support;

import java.util.List;

import org.springframework.batch.item.ClearFailedException;
import org.springframework.batch.item.FlushFailedException;
import org.springframework.batch.item.ItemWriter;

/**
 * Calls a collection of ItemWriters in fixed-order sequence.
 * 
 * The implementation is thread-safe if all delegates are thread-safe.
 * 
 * @author Robert Kasanicky
 */
public class CompositeItemWriter implements ItemWriter {

	private List<ItemWriter> delegates;

	public void setDelegates(List<ItemWriter> delegates) {
		this.delegates = delegates;
	}

	/**
	 * Calls injected ItemProcessors in order.
	 */
	public void write(Object item) throws Exception {
		for (ItemWriter writer : delegates) {
			writer.write(item);
		}
	}

	public void clear() throws ClearFailedException {
		for (ItemWriter writer : delegates) {
			writer.clear();
		}
	}

	public void flush() throws FlushFailedException {
		for (ItemWriter writer : delegates) {
			writer.flush();
		}
	}

}
