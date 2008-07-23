package org.springframework.batch.sample.item.writer;

import java.math.BigDecimal;

import org.springframework.batch.item.support.AbstractItemWriter;
import org.springframework.batch.sample.dao.CustomerCreditDao;
import org.springframework.batch.sample.domain.CustomerCredit;

/**
 * Increases customer's credit by fixed amount.
 * 
 * @author Robert Kasanicky
 */
public class CustomerCreditIncreaseWriter extends AbstractItemWriter<CustomerCredit> {

	public static final BigDecimal FIXED_AMOUNT = new BigDecimal("1000");
	
	private CustomerCreditDao customerCreditDao;
	
	/**
	 * Public setter for the {@link CustomerCreditDao}.
	 * @param customerCreditDao the {@link CustomerCreditDao} to set
	 */
	public void setCustomerCreditDao(CustomerCreditDao customerCreditDao) {
		this.customerCreditDao = customerCreditDao;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.batch.item.processor.DelegatingItemWriter#doProcess(java.lang.Object)
	 */
	public void write(CustomerCredit customerCredit) throws Exception {
		CustomerCredit result = customerCredit.increaseCreditBy(FIXED_AMOUNT);
		customerCreditDao.writeCredit(result);
	}

}
