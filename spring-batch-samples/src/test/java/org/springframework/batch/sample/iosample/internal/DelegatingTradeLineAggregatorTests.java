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

import static junit.framework.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Test;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.batch.sample.domain.trade.Trade;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public class DelegatingTradeLineAggregatorTests {

	@Test
	public void testAggregate() {
		BeanWrapperFieldExtractor<Trade> tradeExtractor = new BeanWrapperFieldExtractor<Trade>();
		tradeExtractor.setNames(new String[] { "isin", "quantity", "price", "customer" });

		DelimitedLineAggregator<Trade> tradeAggregator = new DelimitedLineAggregator<Trade>();
		tradeAggregator.setFieldExtractor(tradeExtractor);

		BeanWrapperFieldExtractor<CustomerCredit> customerExtractor = new BeanWrapperFieldExtractor<CustomerCredit>();
		tradeExtractor.setNames(new String[] { "id", "name", "credit" });

		DelimitedLineAggregator<CustomerCredit> customerAggregator = new DelimitedLineAggregator<CustomerCredit>();
		customerAggregator.setFieldExtractor(customerExtractor);

		DelegatingTradeLineAggregator aggregator = new DelegatingTradeLineAggregator();
		aggregator.setTradeLineAggregator(tradeAggregator);
		aggregator.setCustomerLineAggregator(customerAggregator);

		Trade t = new Trade("ISIN001", 500, new BigDecimal("4.50"), "Customer1");
		assertEquals("ISIN001,500,4.50,Customer1", aggregator.aggregate(t));

		CustomerCredit c = new CustomerCredit(256, "customer1", new BigDecimal("3200.00"));
		assertEquals("256,customer1,3200.00", aggregator.aggregate(c));
	}
}
