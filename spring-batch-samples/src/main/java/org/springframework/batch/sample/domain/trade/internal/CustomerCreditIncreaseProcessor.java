package org.springframework.batch.sample.domain.trade.internal;

import java.math.BigDecimal;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.sample.domain.trade.CustomerCredit;

/**
 * Increases customer's credit by a fixed amount.
 * 
 * @author Robert Kasanicky
 */
public class CustomerCreditIncreaseProcessor implements ItemProcessor<CustomerCredit, CustomerCredit> {
	
	public static final BigDecimal FIXED_AMOUNT = new BigDecimal("5");

	public CustomerCredit process(CustomerCredit item) throws Exception {
		return item.increaseCreditBy(FIXED_AMOUNT);
	}
}
