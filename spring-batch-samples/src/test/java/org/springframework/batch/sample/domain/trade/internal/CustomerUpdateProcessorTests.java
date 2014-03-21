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
		Trade trade = new Trade();
		trade.setCustomer("testCustomerName");
		trade.setPrice(new BigDecimal("123.0"));
		
		CustomerDebitDao dao = new CustomerDebitDao() {
			@Override
			public void write(CustomerDebit customerDebit) {
				assertEquals("testCustomerName", customerDebit.getName());
				assertEquals(new BigDecimal("123.0"), customerDebit.getDebit());
			}			
		};
		
		CustomerUpdateWriter processor = new CustomerUpdateWriter();
		processor.setDao(dao);
		
		processor.write(Collections.singletonList(trade));
	}
}
