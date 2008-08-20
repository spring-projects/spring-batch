package org.springframework.batch.sample.domain.trade.internal;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.batch.sample.domain.trade.CustomerCreditDao;

/**
 * Increases customer's credit by fixed amount.
 * 
 * @author Robert Kasanicky
 */
public class CustomerCreditIncreaseWriter implements ItemWriter<CustomerCredit> {

	public static final BigDecimal FIXED_AMOUNT = new BigDecimal("1000");

	private CustomerCreditDao customerCreditDao;

	/**
	 * Public setter for the {@link CustomerCreditDao}.
	 * @param customerCreditDao the {@link CustomerCreditDao} to set
	 */
	public void setCustomerCreditDao(CustomerCreditDao customerCreditDao) {
		this.customerCreditDao = customerCreditDao;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.processor.DelegatingItemWriter#doProcess
	 * (java.lang.Object)
	 */
	public void write(List<? extends CustomerCredit> customerCredits) throws Exception {
		for (CustomerCredit customerCredit : customerCredits) {
			CustomerCredit result = customerCredit.increaseCreditBy(FIXED_AMOUNT);
			customerCreditDao.writeCredit(result);
		}
	}

}
