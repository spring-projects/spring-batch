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

import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.validator.ValidationException;
import org.springframework.batch.sample.domain.trade.Trade;
import org.springframework.lang.Nullable;

/**
 * Processes the Trade - throwing validation errors if necessary.
 */
public class TradeProcessor implements ItemProcessor<Trade, Trade> {

	private int failure = -1;

	private int index = 0;
	
	private Trade failedItem = null;

	/**
	 * Public setter for the the index on which failure should occur.
	 * 
	 * @param failure the failure to set
	 */
	public void setValidationFailure(int failure) {
		this.failure = failure;
	}

	@Nullable
	@Override
	public Trade process(Trade item) throws Exception {
		if ((failedItem == null && index++ == failure) || (failedItem != null && failedItem.equals(item))) {
			failedItem = item;
			throw new ValidationException("Some bad data for " + failedItem);
		}
		return item;
	}
}
