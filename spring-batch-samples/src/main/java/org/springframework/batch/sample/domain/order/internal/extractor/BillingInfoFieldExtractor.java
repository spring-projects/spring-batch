package org.springframework.batch.sample.domain.order.internal.extractor;

import org.springframework.batch.item.file.transform.FieldExtractor;
import org.springframework.batch.sample.domain.order.BillingInfo;
import org.springframework.batch.sample.domain.order.Order;

/**
 * @author Dan Garrette
 * @since 2.0.1
 */
public class BillingInfoFieldExtractor implements FieldExtractor<Order> {

	public Object[] extract(Order order) {
		BillingInfo billingInfo = order.getBilling();
		return new Object[] { "BILLING:", billingInfo.getPaymentId(), billingInfo.getPaymentDesc() };
	}

}
