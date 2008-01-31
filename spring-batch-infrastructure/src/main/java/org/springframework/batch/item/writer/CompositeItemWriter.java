package org.springframework.batch.item.writer;

import java.util.Iterator;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.stream.CompositeItemStream;

/**
 * Runs a collection of ItemProcessors in fixed-order sequence.
 * 
 * @author Robert Kasanicky
 */
public class CompositeItemWriter extends CompositeItemStream implements ItemWriter {

	/**
	 * Calls injected ItemProcessors in order.
	 */
	public void write(Object data) throws Exception {
		for (Iterator iterator = getDelegates().listIterator(); iterator.hasNext();) {
			((ItemWriter) iterator.next()).write(data);
		}
	}

}
