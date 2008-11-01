package org.springframework.batch.item.support;

import java.util.Iterator;
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

	private List delegates;

	public void setDelegates(List delegates) {
		this.delegates = delegates;
	}

	/**
	 * Calls injected ItemProcessors in order.
	 */
	public void write(Object data) throws Exception {
		for (Iterator iterator = delegates.listIterator(); iterator.hasNext();) {
			((ItemWriter) iterator.next()).write(data);
		}
	}

	public void clear() throws ClearFailedException {
		for (Iterator iterator = delegates.listIterator(); iterator.hasNext();) {
			((ItemWriter) iterator.next()).clear();
		}
	}

	public void flush() throws FlushFailedException {
		for (Iterator iterator = delegates.listIterator(); iterator.hasNext();) {
			((ItemWriter) iterator.next()).flush();
		}
	}

}
