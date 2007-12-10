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
package org.springframework.batch.sample.dao;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

import junit.framework.TestCase;

import org.springframework.batch.io.file.support.transform.DelimitedLineAggregator;
import org.springframework.batch.sample.domain.Address;
import org.springframework.batch.sample.domain.BillingInfo;
import org.springframework.batch.sample.domain.Customer;
import org.springframework.batch.sample.domain.Order;

/**
 * @author Dave Syer
 * 
 */
public class OrderConverterTests extends TestCase {

	private OrderConverter converter = new OrderConverter();

	public void testConvert() throws Exception {
		converter.setAggregators(new HashMap() {
			{
				put("header", new DelimitedLineAggregator());
				put("customer", new DelimitedLineAggregator());
				put("address", new DelimitedLineAggregator());
				put("billing", new DelimitedLineAggregator());
				put("item", new DelimitedLineAggregator());
				put("footer", new DelimitedLineAggregator());
			}
		});
		Order order = new Order();
		order.setOrderDate(new Date());
		order.setCustomer(new Customer());
		order.setBillingAddress(new Address());
		order.setBilling(new BillingInfo());
		order.setLineItems(Collections.EMPTY_LIST);
		order.setTotalPrice(BigDecimal.TEN);
		Object result = converter.convert(order);
		assertTrue(result instanceof Collection);
	}

}
