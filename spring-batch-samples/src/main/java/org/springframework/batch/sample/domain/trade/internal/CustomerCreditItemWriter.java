package org.springframework.batch.sample.domain.trade.internal;

import java.util.List;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.batch.sample.domain.trade.CustomerCreditDao;

/**
 * Delegates actual writing to a custom DAO. 
 * 
 * @author Robert Kasanicky
 */
public class CustomerCreditItemWriter implements ItemWriter<CustomerCredit> {

	private CustomerCreditDao customerCreditDao;

	/**
	 * Public setter for the {@link CustomerCreditDao}.
	 * @param customerCreditDao the {@link CustomerCreditDao} to set
	 */
	public void setCustomerCreditDao(CustomerCreditDao customerCreditDao) {
		this.customerCreditDao = customerCreditDao;
	}

	public void write(List<? extends CustomerCredit> customerCredits) throws Exception {
		for (CustomerCredit customerCredit : customerCredits) {
			customerCreditDao.writeCredit(customerCredit);
		}
	}

}
