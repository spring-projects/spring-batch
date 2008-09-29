/**
 * 
 */
package org.springframework.batch.sample.domain.trade;

/**
 * Interface for logging invalid customers.  Customers may need to be logged because
 * they already existed when attempted to be added.  Or a non existent customer was
 * updated.
 * 
 * @author Lucas Ward
 *
 */
public interface InvalidCustomerLogger {

	void log(CustomerUpdate customerUpdate);
	
}
