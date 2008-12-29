package org.springframework.batch.item.support;

import org.springframework.batch.item.ItemProcessor;

/**
 * Simple {@link ItemProcessor} that does nothing - simply passes its argument
 * through to the caller. Useful as a default when the reader and writer in a
 * business process deal with items of the same type, and no transformations are
 * required.
 * 
 * @author Dave Syer
 * 
 */
public class PassthroughItemProcessor<T> implements ItemProcessor<T, T> {

	/**
	 * Just returns the item back to the caller.
	 * 
	 * @return the item
	 * @see ItemProcessor#process(Object)
	 */
	public T process(T item) throws Exception {
		return item;
	}

}