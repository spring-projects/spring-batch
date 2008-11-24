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

package org.springframework.batch.sample.iosample.internal;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.batch.sample.domain.trade.Trade;
import org.springframework.batch.sample.domain.trade.TradeDao;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public class TradeCustomerItemWriter implements ItemWriter<CustomerCredit> {
	private TradeDao dao;
	private int count;

	public void write(List<? extends CustomerCredit> items) throws Exception {
		for (CustomerCredit c : items) {
			Trade t = new Trade("ISIN" + count++, 100, new BigDecimal("1.50"), c.getName());
			this.dao.writeTrade(t);
		}
	}

	public void setDao(TradeDao dao) {
		this.dao = dao;
	}
}
