/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.sample.domain.trade.internal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.annotation.AfterWrite;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.WriteFailedException;
import org.springframework.batch.sample.domain.trade.Trade;
import org.springframework.batch.sample.domain.trade.TradeDao;
import org.springframework.util.Assert;

/**
 * Delegates the actual writing to custom DAO delegate. Allows configurable
 * exception raising for testing skip and restart.
 */
public class TradeWriter extends ItemStreamSupport implements ItemWriter<Trade> {

	private static Log log = LogFactory.getLog(TradeWriter.class);

	private static final String TOTAL_AMOUNT_KEY = "TOTAL_AMOUNT";

	private TradeDao dao;

	private List<String> failingCustomers = new ArrayList<String>();

	private BigDecimal totalPrice = BigDecimal.ZERO;

	public void write(List<? extends Trade> trades) {

		for (Trade trade : trades) {

			log.debug(trade);

			dao.writeTrade(trade);

			Assert.notNull(trade.getPrice()); // There must be a price to total

			if (this.failingCustomers.contains(trade.getCustomer())) {
				throw new WriteFailedException("Something unexpected happened!");
			}
		}

	}

	@AfterWrite
	public void updateTotalPrice(List<Trade> trades) {
		for (Trade trade : trades) {
			this.totalPrice = this.totalPrice.add(trade.getPrice());
		}
	}

	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		if (executionContext.containsKey(TOTAL_AMOUNT_KEY)) {
			this.totalPrice = (BigDecimal) executionContext.get(TOTAL_AMOUNT_KEY);
		}
		else {
			//
			// Fresh run. Disregard old state.
			//
			this.totalPrice = BigDecimal.ZERO;
		}
	}

	@Override
	public void update(ExecutionContext executionContext) {
		executionContext.put(TOTAL_AMOUNT_KEY, this.totalPrice);
	}

	public BigDecimal getTotalPrice() {
		return totalPrice;
	}

	public void setDao(TradeDao dao) {
		this.dao = dao;
	}

	/**
	 * Public setter for the the customers on which failure should occur.
	 * 
	 * @param failingCustomers The customers to fail on
	 */
	public void setFailingCustomers(List<String> failingCustomers) {
		this.failingCustomers = failingCustomers;
	}
}
