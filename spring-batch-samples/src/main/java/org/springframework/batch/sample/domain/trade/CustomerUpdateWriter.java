/**
 * 
 */
package org.springframework.batch.sample.domain.trade;

import java.util.List;

import org.springframework.batch.item.ItemWriter;

/**
 * @author Lucas Ward
 *
 */
public class CustomerUpdateWriter implements ItemWriter<CustomerUpdate> {

	private CustomerDao customerDao;
	
	public void write(List<? extends CustomerUpdate> items) throws Exception {
		for(CustomerUpdate customerUpdate : items){
			if(customerUpdate.getOperation() == CustomerOperation.ADD){
				customerDao.insertCustomer(customerUpdate.getCustomerName(), customerUpdate.getCredit());
			}
			else if(customerUpdate.getOperation() == CustomerOperation.UPDATE){
				customerDao.updateCustomer(customerUpdate.getCustomerName(), customerUpdate.getCredit());
			}
		}
		
		//flush and/or clear resources
	}
	
	public void setCustomerDao(CustomerDao customerDao) {
		this.customerDao = customerDao;
	}

}
