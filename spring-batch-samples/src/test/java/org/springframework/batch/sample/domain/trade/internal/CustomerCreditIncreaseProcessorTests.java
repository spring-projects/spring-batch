package org.springframework.batch.sample.domain.trade.internal;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Test;
import org.springframework.batch.sample.domain.trade.CustomerCredit;

/**
 * Tests for {@link CustomerCreditItemWriter}.
 * 
 * @author Robert Kasanicky
 */
public class CustomerCreditIncreaseProcessorTests {

	private CustomerCreditIncreaseProcessor tested = new CustomerCreditIncreaseProcessor();

	/*
	 * Increases customer's credit by fixed value
	 */
	@Test
	public void testProcess() throws Exception {
		
		final BigDecimal oldCredit = new BigDecimal(10.54);
		CustomerCredit customerCredit = new CustomerCredit();
		customerCredit.setCredit(oldCredit);
		
		assertEquals(oldCredit.add(CustomerCreditIncreaseProcessor.FIXED_AMOUNT),tested.process(customerCredit).getCredit());
	}
}
