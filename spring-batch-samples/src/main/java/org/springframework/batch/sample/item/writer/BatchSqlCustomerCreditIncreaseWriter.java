package org.springframework.batch.sample.item.writer;

import java.math.BigDecimal;

import org.springframework.batch.item.ClearFailedException;
import org.springframework.batch.item.FlushFailedException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BatchSqlUpdateItemWriter;
import org.springframework.batch.sample.domain.CustomerCredit;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Increases customer's credit by fixed amount, delegating to a
 * {@link BatchSqlUpdateItemWriter} to push the result out to persistent
 * storage.
 * 
 * @author Dave Syer
 */
public class BatchSqlCustomerCreditIncreaseWriter implements ItemWriter<CustomerCredit>, InitializingBean {

	private ItemWriter<CustomerCredit> delegate;

	public static final BigDecimal FIXED_AMOUNT = new BigDecimal(1000);

	/**
	 * Public setter for the {@link ItemWriter}, which must be an instance of
	 * {@link BatchSqlUpdateItemWriter}.
	 * @param delegate the delegate to set
	 */
	public void setDelegate(ItemWriter<CustomerCredit> delegate) {
		this.delegate = delegate;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.state(delegate instanceof BatchSqlUpdateItemWriter, "Delegate must be set and must be an instance of BatchSqlUpdateItemWriter");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.item.processor.DelegatingItemWriter#doProcess(java.lang.Object)
	 */
	public void write(CustomerCredit customerCredit) throws Exception {
		delegate.write(customerCredit);
	}

	public void clear() throws ClearFailedException {
		delegate.clear();
	}

	public void flush() throws FlushFailedException {
		delegate.flush();
	}

}
