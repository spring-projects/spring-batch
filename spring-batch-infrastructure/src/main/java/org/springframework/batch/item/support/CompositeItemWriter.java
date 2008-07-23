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
public class CompositeItemWriter<T> implements ItemWriter<T> {

	private List<ItemWriter<T>> delegates;

	public void setDelegates(List<ItemWriter<T>> delegates) {
		this.delegates = delegates;
	}

	/**
	 * Calls injected ItemProcessors in order.
	 */
	public void write(T item) throws Exception {
		for (ItemWriter<T> writer : delegates) {
			writer.write(item);
		}
	}

	public void clear() throws ClearFailedException {
		for (ItemWriter<T> writer : delegates) {
			writer.clear();
		}
	}

	public void flush() throws FlushFailedException {
		for (ItemWriter<T> writer : delegates) {
			writer.flush();
		}
	}

}
