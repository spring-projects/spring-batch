package org.springframework.batch.sample.domain.order.internal.extractor;

import org.springframework.batch.item.file.transform.FieldExtractor;
import org.springframework.batch.sample.domain.order.Customer;
import org.springframework.batch.sample.domain.order.Order;

/**
 * @author Dan Garrette
 * @since 2.0.1
 */
public class CustomerFieldExtractor implements FieldExtractor<Order> {

	public Object[] extract(Order order) {
		Customer customer = order.getCustomer();
		return new Object[] { "CUSTOMER:", customer.getRegistrationId(), emptyIfNull(customer.getFirstName()),
				emptyIfNull(customer.getMiddleName()), emptyIfNull(customer.getLastName()) };
	}

	private String emptyIfNull(String s) {
		return s != null ? s : "";
	}

}
