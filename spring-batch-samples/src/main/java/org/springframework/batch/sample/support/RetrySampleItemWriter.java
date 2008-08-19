package org.springframework.batch.sample.support;

import java.util.List;

import org.springframework.batch.item.support.AbstractItemWriter;

/**
 * Simulates temporary output trouble - requires to retry 3 times to pass
 * successfully.
 * 
 * @author Robert Kasanicky
 */
public class RetrySampleItemWriter<T> extends AbstractItemWriter<T> {

	private int counter = 0;

	public void write(List<? extends T> items) throws Exception {
		int current = counter;
		counter += items.size();
		if (current < 3 && (counter >= 2 || counter >= 3)) {
			throw new RuntimeException("Temporary error");
		}
	}

	/**
	 * @return number of times {@link #write(List)} method was called.
	 */
	public int getCounter() {
		return counter;
	}

}
