package org.springframework.batch.sample.domain.order.internal.extractor;

import org.springframework.batch.item.file.transform.FieldExtractor;
import org.springframework.batch.sample.domain.order.Address;
import org.springframework.batch.sample.domain.order.Order;

/**
 * @author Dan Garrette
 * @since 2.0.1
 */
public class AddressFieldExtractor implements FieldExtractor<Order> {

	public Object[] extract(Order order) {
		Address address = order.getBillingAddress();
		return new Object[] { "ADDRESS:", address.getAddrLine1(), address.getCity(), address.getZipCode() };
	}

}
