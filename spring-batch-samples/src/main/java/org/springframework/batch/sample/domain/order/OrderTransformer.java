/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.sample.domain.order;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.batch.item.file.mapping.DefaultFieldSet;
import org.springframework.batch.item.file.mapping.FieldSet;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.batch.item.transform.ItemTransformer;

/**
 * Converts <code>Order</code> object to a String.
 * @author Dave Syer
 */
public class OrderTransformer implements ItemTransformer<Order, List<String>> {

	/**
	 * Aggregators for all types of lines in the output file
	 */
	private Map<String, LineAggregator> aggregators;

	/**
	 * Converts information from an Order object to a collection of Strings for
	 * output.
	 */
	public List<String> transform(Order order) {

		List<String> result = new ArrayList<String>();

		result.add(getAggregator("header").aggregate(OrderFormatterUtils.headerArgs(order)));
		result.add(getAggregator("customer").aggregate(OrderFormatterUtils.customerArgs(order)));
		result.add(getAggregator("address").aggregate(OrderFormatterUtils.billingAddressArgs(order)));
		result.add(getAggregator("billing").aggregate(OrderFormatterUtils.billingInfoArgs(order)));

		List<LineItem> items = order.getLineItems();

		for (LineItem lineItem : items) {
			result.add(getAggregator("item").aggregate(OrderFormatterUtils.lineItemArgs(lineItem)));
		}

		result.add(getAggregator("footer").aggregate(OrderFormatterUtils.footerArgs(order)));

		return result;
	}

	public void setAggregators(Map<String, LineAggregator> aggregators) {
		this.aggregators = aggregators;
	}

	private LineAggregator getAggregator(String name) {
		return (LineAggregator) aggregators.get(name);
	}

	/**
	 * Utility class encapsulating formatting of <code>Order</code> and its
	 * nested objects.
	 */
	private static class OrderFormatterUtils {

		private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");

		static FieldSet headerArgs(Order order) {
			return new DefaultFieldSet(new String[] { "BEGIN_ORDER:", String.valueOf(order.getOrderId()),
					dateFormat.format(order.getOrderDate()) });
		}

		static FieldSet footerArgs(Order order) {
			return new DefaultFieldSet(new String[] { "END_ORDER:", order.getTotalPrice().toString() });
		}

		static FieldSet customerArgs(Order order) {
			Customer customer = order.getCustomer();

			return new DefaultFieldSet(new String[] { "CUSTOMER:", String.valueOf(customer.getRegistrationId()),
					customer.getFirstName(), customer.getMiddleName(), customer.getLastName() });
		}

		static FieldSet lineItemArgs(LineItem item) {
			return new DefaultFieldSet(new String[] { "ITEM:", String.valueOf(item.getItemId()),
					item.getPrice().toString() });
		}

		static FieldSet billingAddressArgs(Order order) {
			Address address = order.getBillingAddress();

			return new DefaultFieldSet(new String[] { "ADDRESS:", address.getAddrLine1(), address.getCity(),
					address.getZipCode() });
		}

		static FieldSet billingInfoArgs(Order order) {
			BillingInfo billingInfo = order.getBilling();

			return new DefaultFieldSet(new String[] { "BILLING:", billingInfo.getPaymentId(),
					billingInfo.getPaymentDesc() });
		}
	}

}
