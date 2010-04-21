package org.springframework.batch.core.partition;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;

/**
 * {@link ItemReader} with hard-coded input data.
 */
public class ExampleItemReader implements ItemReader<String>, ItemStream {

	private Log logger = LogFactory.getLog(getClass());

	private String[] input = { "Hello", "world!", "Go", "on", "punk", "make", "my", "day!" };

	private int index = 0;

	private int min = 0;

	private int max = Integer.MAX_VALUE;

	public static volatile boolean fail = false;

	/**
	 * @param min the min to set
	 */
	public void setMin(int min) {
		this.min = min;
	}

	/**
	 * @param max the max to set
	 */
	public void setMax(int max) {
		this.max = max;
	}

	/**
	 * Reads next record from input
	 */
	public String read() throws Exception {
		if (index >= input.length || index >= max) {
			return null;
		}
		logger.info(String.format("Processing input index=%s, item=%s, in (%s)", index, input[index], this));
		if (fail && index == 4) {
			synchronized (ExampleItemReader.class) {
				if (fail) {
					// Only fail once per flag setting...
					fail = false;
					logger.info(String.format("Throwing exception index=%s, item=%s, in (%s)", index, input[index],
							this));
					index++;
					throw new RuntimeException("Planned failure");
				}
			}
		}
		return input[index++];
	}

	public void close() throws ItemStreamException {
	}

	public void open(ExecutionContext executionContext) throws ItemStreamException {
		index = (int) executionContext.getLong("POSITION", min);
	}

	public void update(ExecutionContext executionContext) throws ItemStreamException {
		executionContext.putLong("POSITION", index);
	}

}
