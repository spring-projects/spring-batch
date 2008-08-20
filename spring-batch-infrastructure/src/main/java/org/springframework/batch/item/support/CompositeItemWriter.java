package org.springframework.batch.item.support;

import java.util.Arrays;
import java.util.List;

import org.springframework.batch.item.ItemWriter;

/**
 * Calls a collection of ItemWriters in fixed-order sequence.
 * 
 * The implementation is thread-safe if all delegates are thread-safe.
 * 
 * @author Robert Kasanicky
 * @author Dave Syer
 */
public class CompositeItemWriter<T> implements ItemWriter<T> {

	private List<ItemWriter<? super T>> delegates;

	public void setDelegates(ItemWriter<? super T>[] delegates) {
		this.delegates = Arrays.asList(delegates);
	}

	/**
	 * Calls injected ItemProcessors in order.
	 */
	public void write(List<? extends  T> item) throws Exception {
		for (ItemWriter<? super T> writer : delegates) {
			writer.write(item);
		}
	}

}
