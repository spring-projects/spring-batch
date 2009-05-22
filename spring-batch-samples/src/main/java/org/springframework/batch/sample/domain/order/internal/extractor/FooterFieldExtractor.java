package org.springframework.batch.sample.domain.order.internal.extractor;

import org.springframework.batch.item.file.transform.FieldExtractor;
import org.springframework.batch.sample.domain.order.Order;

/**
 * @author Dan Garrette
 * @since 2.0.1
 */
public class FooterFieldExtractor implements FieldExtractor<Order> {

	public Object[] extract(Order order) {
		return new Object[] { "END_ORDER:", order.getTotalPrice() };
	}

}
