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

package org.springframework.batch.sample.domain.order.internal;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.batch.sample.domain.order.Address;
import org.springframework.batch.sample.domain.order.BillingInfo;
import org.springframework.batch.sample.domain.order.Customer;
import org.springframework.batch.sample.domain.order.LineItem;
import org.springframework.batch.sample.domain.order.Order;

/**
 * Converts <code>Order</code> object to a list of strings.
 * 
 * @author Dave Syer
 */
public class OrderProcessor implements ItemProcessor<Order, List<String>> {

	/**
	 * Aggregators for all types of lines in the output file
	 */
	private Map<String, LineAggregator<Object[]>> aggregators;

	/**
	 * Converts information from an Order object to a collection of Strings for
	 * output.
	 * @throws Exception 
	 */
	public List<String> process(Order order) throws Exception {

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

	public void setAggregators(Map<String, LineAggregator<Object[]>> aggregators) {
		this.aggregators = aggregators;
	}

	private LineAggregator<Object[]> getAggregator(String name) {
		return aggregators.get(name);
	}

	/**
	 * Utility class encapsulating formatting of <code>Order</code> and its
	 * nested objects.
	 */
	private static class OrderFormatterUtils {

		private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");

		static Object[] headerArgs(Order order) {
			return new Object[] { "BEGIN_ORDER:", String.valueOf(order.getOrderId()),
					dateFormat.format(order.getOrderDate()) };
		}

		static Object[] footerArgs(Order order) {
			return new Object[] { "END_ORDER:", order.getTotalPrice().toString() };
		}

		static Object[] customerArgs(Order order) {
			Customer customer = order.getCustomer();

			return new Object[] { "CUSTOMER:", String.valueOf(customer.getRegistrationId()), customer.getFirstName(),
					customer.getMiddleName(), customer.getLastName() };
		}

		static Object[] lineItemArgs(LineItem item) {
			return new Object[] { "ITEM:", String.valueOf(item.getItemId()), item.getPrice().toString() };
		}

		static Object[] billingAddressArgs(Order order) {
			Address address = order.getBillingAddress();

			return new Object[] { "ADDRESS:", address.getAddrLine1(), address.getCity(), address.getZipCode() };
		}

		static Object[] billingInfoArgs(Order order) {
			BillingInfo billingInfo = order.getBilling();

			return new Object[] { "BILLING:", billingInfo.getPaymentId(), billingInfo.getPaymentDesc() };
		}
	}

}
