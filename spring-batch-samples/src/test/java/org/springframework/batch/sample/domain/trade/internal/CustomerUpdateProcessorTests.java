package org.springframework.batch.sample.domain.trade.internal;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.Test;
import org.springframework.batch.sample.domain.trade.CustomerDebit;
import org.springframework.batch.sample.domain.trade.CustomerDebitDao;
import org.springframework.batch.sample.domain.trade.Trade;

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
		processor.write(Collections.singletonList(trade));
	}
}
