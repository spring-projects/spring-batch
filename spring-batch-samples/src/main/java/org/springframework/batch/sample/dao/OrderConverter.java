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

package org.springframework.batch.sample.dao;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.batch.io.file.support.transform.Converter;
import org.springframework.batch.io.file.support.transform.LineAggregator;
import org.springframework.batch.sample.domain.Address;
import org.springframework.batch.sample.domain.BillingInfo;
import org.springframework.batch.sample.domain.Customer;
import org.springframework.batch.sample.domain.LineItem;
import org.springframework.batch.sample.domain.Order;


/**
 * Converts <code>Order</code> object to a String.
 * @author Dave Syer
 */
public class OrderConverter implements Converter {

    /**
     * Aggregators for all types of lines in the output file
     */
    private Map aggregators;

    /**
     * Converts information from an Order object to a collection of Strings for output.
     */
    public Object convert(Object data) {
        Order order = (Order) data;
        
        List result = new ArrayList();

        result.add(getAggregator("header").aggregate(OrderFormatterUtils.headerArgs(order)));
        result.add(getAggregator("customer").aggregate(OrderFormatterUtils.customerArgs(order)));
        result.add(getAggregator("address").aggregate(OrderFormatterUtils.billingAddressArgs(order)));
        result.add(getAggregator("billing").aggregate(OrderFormatterUtils.billingInfoArgs(order)));

        List items = order.getLineItems();
        LineItem item;

        for (int i = 0; i < items.size(); i++) {
            item = (LineItem) items.get(i);
            result.add(getAggregator("item").aggregate(OrderFormatterUtils.lineItemArgs(item)));
        }

        result.add(getAggregator("footer").aggregate(OrderFormatterUtils.footerArgs(order)));
        
        return result;
    }
    
    public void setAggregators(Map aggregators) {
        this.aggregators = aggregators;
    }
    
    private LineAggregator getAggregator(String name) {
		return (LineAggregator) aggregators.get(name);
	}

    /**
     * Utility class encapsulating formatting of <code>Order</code> and its nested objects.
     */
	private static class OrderFormatterUtils {
		
		private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");

		static String[] headerArgs(Order order) {
			return new String[] { "BEGIN_ORDER:", String.valueOf(order.getOrderId()), dateFormat.format(order.getOrderDate()) };
		}

		static String[] footerArgs(Order order) {
			return new String[] { "END_ORDER:", order.getTotalPrice().toString() };
		}

		static String[] customerArgs(Order order) {
			Customer customer = order.getCustomer();

			return new String[] { "CUSTOMER:", String.valueOf(customer.getRegistrationId()), customer.getFirstName(),
					customer.getMiddleName(), customer.getLastName() };
		}

		static String[] lineItemArgs(LineItem item) {
			return new String[] { "ITEM:", String.valueOf(item.getItemId()), item.getPrice().toString() };
		}

		static String[] billingAddressArgs(Order order) {
			Address address = order.getBillingAddress();

			return new String[] { "ADDRESS:", address.getAddrLine1(), address.getCity(), address.getZipCode() };
		}

		static String[] billingInfoArgs(Order order) {
			BillingInfo billingInfo = order.getBilling();

			return new String[] { "BILLING:", billingInfo.getPaymentId(), billingInfo.getPaymentDesc() };
		}
	}
	
}
