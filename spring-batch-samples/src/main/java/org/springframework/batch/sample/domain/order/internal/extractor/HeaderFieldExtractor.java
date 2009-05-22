package org.springframework.batch.sample.domain.order.internal.extractor;

import java.text.SimpleDateFormat;

import org.springframework.batch.item.file.transform.FieldExtractor;
import org.springframework.batch.sample.domain.order.Order;

/**
 * @author Dan Garrette
 * @since 2.0.1
 */
public class HeaderFieldExtractor implements FieldExtractor<Order> {

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");

	public Object[] extract(Order order) {
		return new Object[] { "BEGIN_ORDER:", order.getOrderId(), dateFormat.format(order.getOrderDate()) };
	}

}
