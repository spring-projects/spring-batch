package org.springframework.batch.sample.domain.trade;

import java.math.BigDecimal;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.MarkFailedException;
import org.springframework.batch.item.ResetFailedException;

/**
 * Generates configurable number of {@link Trade} items.
 * 
 * @author Robert Kasanicky
 */
public class GeneratingTradeItemReader implements ItemReader<Trade> {

	private int limit = 1;
	
	private int counter = 0;

	private int marked;
	
	public Trade read() throws Exception {
		if (counter < limit) {
			counter++;
			return new Trade(
					"isin" + counter, 
					counter, 
					new BigDecimal(counter), 
					"customer" + counter);
		}
		return null;
	}

	/**
	 * @param limit number of items that will be generated
	 * (null returned on consecutive calls).
	 */
	public void setLimit(int limit) {
		this.limit = limit;
	}

	public int getCounter() {
		return counter;
	}

	public int getLimit() {
		return limit;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStream#mark()
	 */
	public void mark() throws MarkFailedException {
		this.marked = this.counter;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStream#reset()
	 */
	public void reset() throws ResetFailedException {
		this.counter = this.marked;
	}

}
