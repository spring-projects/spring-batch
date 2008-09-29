/**
 * 
 */
package org.springframework.batch.sample.domain.trade;

import java.math.BigDecimal;

/**
 * @author Lucas Ward
 *
 */
public interface CustomerDao {

	CustomerCredit getCustomerByName(String name);

	void insertCustomer(String name, BigDecimal credit);
	
	void updateCustomer(String name, BigDecimal credit);
}
