/**
 * 
 */
package org.springframework.batch.sample.domain.trade;

import static org.springframework.batch.sample.domain.trade.CustomerOperation.*;

import org.springframework.batch.item.ItemProcessor;

/**
 * @author Lucas Ward
 *
 */
public class CustomerUpdateProcessor implements ItemProcessor<CustomerUpdate, CustomerUpdate>{

	CustomerDao customerDao;
	InvalidCustomerLogger invalidCustomerLogger;
	
	public CustomerUpdate process(CustomerUpdate item) throws Exception {
		
		if(item.getOperation() == DELETE){
			//delete is not supported
			invalidCustomerLogger.log(item);
			return null;
		}
		
		CustomerCredit customerCredit = customerDao.getCustomerByName(item.getCustomerName());
		
		if(item.getOperation() == ADD && customerCredit == null){
			return item;
		}
		else if(item.getOperation() == ADD && customerCredit != null){
			//veto processing
			invalidCustomerLogger.log(item);
			return null;
		}
		
		if(item.getOperation() == UPDATE && customerCredit != null){
			return item;
		}
		else if(item.getOperation() == UPDATE && customerCredit == null){
			//veto processing
			invalidCustomerLogger.log(item);
			return null;
		}
		
		//if an item makes it through all these checks it can be assumed to be bad, logged, and skipped
		invalidCustomerLogger.log(item);
		return null;
	}
	
	public void setCustomerDao(CustomerDao customerDao) {
		this.customerDao = customerDao;
	}
	
	public void setInvalidCustomerLogger(
			InvalidCustomerLogger invalidCustomerLogger) {
		this.invalidCustomerLogger = invalidCustomerLogger;
	}

}
