/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.sample.domain.order;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.batch.sample.domain.order.internal.OrderProcessor;

public class FlatFileOrderAggregatorTests {

	@Test
	public void testWrite() throws Exception {

		// Create and set-up Order
		Order order = new Order();

		order.setOrderDate(new GregorianCalendar(2007, GregorianCalendar.JUNE, 1).getTime());
		order.setCustomer(new Customer());
		order.setBilling(new BillingInfo());
		order.setBillingAddress(new Address());
		List<LineItem> lineItems = new ArrayList<LineItem>();
		LineItem item = new LineItem();
		item.setPrice(BigDecimal.valueOf(0));
		lineItems.add(item);
		lineItems.add(item);
		order.setLineItems(lineItems);
		order.setTotalPrice(BigDecimal.valueOf(0));

		// create aggregator stub
		LineAggregator<Object[]> aggregator = new DelimitedLineAggregator<Object[]>();

		// create map of aggregators and set it to writer
		Map<String, LineAggregator<Object[]>> aggregators = new HashMap<String, LineAggregator<Object[]>>();

		OrderProcessor converter = new OrderProcessor();
		aggregators.put("header", aggregator);
		aggregators.put("customer", aggregator);
		aggregators.put("address", aggregator);
		aggregators.put("billing", aggregator);
		aggregators.put("item", aggregator);
		aggregators.put("footer", aggregator);
		converter.setAggregators(aggregators);

		// call tested method
		List<String> list = converter.process(order);

		// verify method calls
		assertEquals(7, list.size());
		assertEquals("BEGIN_ORDER:,0,2007/06/01", list.get(0));

	}

}
