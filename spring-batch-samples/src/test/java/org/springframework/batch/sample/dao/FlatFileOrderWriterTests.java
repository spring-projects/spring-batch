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
package org.springframework.batch.sample.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.batch.item.support.AbstractItemWriter;
import org.springframework.batch.sample.StubLineAggregator;
import org.springframework.batch.sample.domain.Address;
import org.springframework.batch.sample.domain.BillingInfo;
import org.springframework.batch.sample.domain.Customer;
import org.springframework.batch.sample.domain.LineItem;
import org.springframework.batch.sample.domain.Order;
import org.junit.Before;
import org.junit.Test;

public class FlatFileOrderWriterTests {

	List<Object> list = new ArrayList<Object>();
	
	private ItemWriter output = new AbstractItemWriter() {
		public void write(Object output) {
			list.add(output);
		}
	};

	private FlatFileOrderWriter writer;
	
	@Before
	public void setUp() throws Exception {
		//create new writer
		writer = new FlatFileOrderWriter();
		writer.setDelegate(output);
	}

	@Test
	public void testWrite() throws Exception {
		
		//Create and set-up Order
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
		
		//create aggregator stub
		LineAggregator aggregator = new StubLineAggregator();
		
		//create map of aggregators and set it to writer
		Map<String, LineAggregator> aggregators = new HashMap<String, LineAggregator>();
		
		OrderTransformer converter = new OrderTransformer();
		aggregators.put("header", aggregator);
		aggregators.put("customer", aggregator);
		aggregators.put("address", aggregator);
		aggregators.put("billing", aggregator);
		aggregators.put("item", aggregator);
		aggregators.put("footer", aggregator);
		converter.setAggregators(aggregators);
		writer.setTransformer(converter);
				
		//call tested method
		writer.write(order);
		
		//verify method calls
		assertEquals(1, list.size());
		assertTrue(list.get(0) instanceof List);
		assertEquals("02007/06/01", ((List<?>) list.get(0)).get(0));
		
	}

}
