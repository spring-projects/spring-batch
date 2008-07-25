package org.springframework.batch.sample.item.writer;

import static org.junit.Assert.*;
import org.junit.Test;

import java.math.BigDecimal;

import org.springframework.batch.sample.dao.CustomerDebitDao;
import org.springframework.batch.sample.trade.CustomerDebit;
import org.springframework.batch.sample.trade.CustomerUpdateWriter;
import org.springframework.batch.sample.trade.Trade;

public class CustomerUpdateProcessorTests {

	@Test
	public void testProcess() {
		
		//create trade object
		Trade trade = new Trade();
		trade.setCustomer("testCustomerName");
		trade.setPrice(new BigDecimal(123.0));
		
		//create dao
		CustomerDebitDao dao = new CustomerDebitDao() {
			public void write(CustomerDebit customerDebit) {
				assertEquals("testCustomerName", customerDebit.getName());
				assertEquals(new BigDecimal(123.0), customerDebit.getDebit());
			}			
		};
		
		//create processor and set dao
		CustomerUpdateWriter processor = new CustomerUpdateWriter();
		processor.setDao(dao);
		
		//call tested method - see asserts in dao.write() method
		processor.write(trade);
	}
}
