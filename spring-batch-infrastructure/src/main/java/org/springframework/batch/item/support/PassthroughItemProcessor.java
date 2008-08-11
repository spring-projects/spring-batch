package org.springframework.batch.item.support;

import org.springframework.batch.item.ItemProcessor;

/**
 * @author Dave Syer
 *
 */
public class PassthroughItemProcessor<T> implements ItemProcessor<T, T> {
	public T process(T item) throws Exception {
		return item;
	}
}