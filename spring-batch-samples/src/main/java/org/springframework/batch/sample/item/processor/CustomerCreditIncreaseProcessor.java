package org.springframework.batch.sample.item.processor;

import java.math.BigDecimal;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.sample.domain.CustomerCredit;

/**
 * Increases customer's credit by fixed amount.
 * 
 * @author Robert Kasanicky
 */
public class CustomerCreditIncreaseProcessor implements ItemProcessor{

	public static final BigDecimal FIXED_AMOUNT = new BigDecimal(1000);
	
	private ItemWriter outputSource;
	
	public void process(Object data) throws Exception {
		CustomerCredit customerCredit = (CustomerCredit) data;
		customerCredit.increaseCreditBy(FIXED_AMOUNT);
		outputSource.write(customerCredit);
	}

	public void setOutputSource(ItemWriter outputSource) {
		this.outputSource = outputSource;
	}

}
