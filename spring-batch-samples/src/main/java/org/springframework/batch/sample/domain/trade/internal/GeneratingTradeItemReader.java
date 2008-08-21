package org.springframework.batch.sample.domain.trade.internal;

import java.math.BigDecimal;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.sample.domain.trade.Trade;

/**
 * Generates configurable number of {@link Trade} items.
 * 
 * @author Robert Kasanicky
 */
public class GeneratingTradeItemReader implements ItemReader<Trade> {

	private int limit = 1;
	
	private int counter = 0;

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

}
