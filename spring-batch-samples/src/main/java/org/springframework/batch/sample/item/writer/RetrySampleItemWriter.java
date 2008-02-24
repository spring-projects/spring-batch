package org.springframework.batch.sample.item.writer;

import org.springframework.batch.item.writer.AbstractItemWriter;

/**
 * Simulates temporary output trouble - requires to
 * retry 3 times to pass successfully.
 * 
 * @author Robert Kasanicky
 */
public class RetrySampleItemWriter extends AbstractItemWriter {

	private int counter = 0;
	
	public void write(Object data) throws Exception {
		counter++;
		if (counter == 2 || counter == 3) {
			throw new RuntimeException("Temporary error");
		}
	}

	/**
	 * @return number of times {@link #process(Object)} method was called.
	 */
	public int getCounter() {
		return counter;
	}

}
