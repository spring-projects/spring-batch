/*
 * Copyright 2006-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.sample.domain.trade.internal;

import java.math.BigDecimal;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.sample.domain.trade.Trade;
import org.springframework.lang.Nullable;

/**
 * Generates configurable number of {@link Trade} items.
 * 
 * @author Robert Kasanicky
 */
public class GeneratingTradeItemReader implements ItemReader<Trade> {

	private int limit = 1;
	
	private int counter = 0;

	@Nullable
	@Override
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

	public void resetCounter()
	{
		this.counter = 0;
	}
}
