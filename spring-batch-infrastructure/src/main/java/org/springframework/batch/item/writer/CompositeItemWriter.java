package org.springframework.batch.item.writer;

import java.util.Iterator;
import java.util.List;

import org.springframework.batch.item.AbstractItemWriter;
import org.springframework.batch.item.ItemWriter;

/**
 * Runs a collection of ItemProcessors in fixed-order sequence.
 * 
 * @author Robert Kasanicky
 */
public class CompositeItemWriter extends AbstractItemWriter {

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

}
