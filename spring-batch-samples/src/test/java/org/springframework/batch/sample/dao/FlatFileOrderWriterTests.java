package org.springframework.batch.sample.dao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.springframework.batch.item.AbstractItemWriter;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.batch.sample.StubLineAggregator;
import org.springframework.batch.sample.domain.Address;
import org.springframework.batch.sample.domain.BillingInfo;
import org.springframework.batch.sample.domain.Customer;
import org.springframework.batch.sample.domain.LineItem;
import org.springframework.batch.sample.domain.Order;

public class FlatFileOrderWriterTests extends TestCase {

	List list = new ArrayList();
	
	private ItemWriter output = new AbstractItemWriter() {
		public void write(Object output) {
			list.add(output);
		}
	};

	private FlatFileOrderWriter writer;
	
	public void setUp() throws Exception {
		super.setUp();		
		//create new writer
		writer = new FlatFileOrderWriter();
		writer.setDelegate(output);
	}

	public void testWrite() throws Exception {
		
		//Create and set-up Order
		Order order = new Order();
		
		order.setOrderDate(new GregorianCalendar(2007, GregorianCalendar.JUNE, 1).getTime());
		order.setCustomer(new Customer());
		order.setBilling(new BillingInfo());
		order.setBillingAddress(new Address());
		List lineItems = new ArrayList();
		LineItem item = new LineItem();
		item.setPrice(BigDecimal.valueOf(0));
		lineItems.add(item);
		lineItems.add(item);
		order.setLineItems(lineItems);
		order.setTotalPrice(BigDecimal.valueOf(0));
		
		//create aggregator stub
		LineAggregator aggregator = new StubLineAggregator();
		
		//create map of aggregators and set it to writer
		Map aggregators = new HashMap();
		
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
		assertEquals("02007/06/01", ((List) list.get(0)).get(0));
		
	}

}
