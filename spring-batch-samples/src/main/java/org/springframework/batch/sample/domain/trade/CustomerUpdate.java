/**
 * 
 */
package org.springframework.batch.sample.domain.trade;

import java.math.BigDecimal;

/**
 * Immutable Value Object representing an update to the customer as stored in the database. 
 * This object has the customer name, credit ammount, and the operation to be performed
 * on them.  In the case of an add, a new customer will be entered with the appropriate 
 * credit.  In the case of an update, the customer's credit is considered an absolute update.
 * Deletes are currently not supported, but can still be read in from a file.
 * 
 * @author Lucas Ward
 * @since 2.0
 */
public class CustomerUpdate {

	private final CustomerOperation operation;
	private final String customerName;
	private final BigDecimal credit;
	
	public CustomerUpdate(CustomerOperation operation, String customerName, BigDecimal credit) {
		this.operation = operation;
		this.customerName = customerName;
		this.credit = credit;
	}

	public CustomerOperation getOperation() {
		return operation;
	}

	public String getCustomerName() {
		return customerName;
	}

	public BigDecimal getCredit() {
		return credit;
	}
	
	@Override
	public String toString() {
		return "Customer Update, name: [" + customerName + "], operation: [" + operation + "], credit: [" + credit + "]";
	}
}