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

import java.util.Map;

import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.batch.sample.domain.order.LineItem;
import org.springframework.batch.sample.domain.order.Order;

/**
 * Converts <code>Order</code> object to a list of strings.
 * 
 * @author Dave Syer
 * @author Dan Garrette
 */
public class OrderLineAggregator implements LineAggregator<Order> {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	private Map<String, LineAggregator<Object>> aggregators;

	public String aggregate(Order order) {
		StringBuilder result = new StringBuilder();

		result.append(aggregators.get("header").aggregate(order) + LINE_SEPARATOR);
		result.append(aggregators.get("customer").aggregate(order) + LINE_SEPARATOR);
		result.append(aggregators.get("address").aggregate(order) + LINE_SEPARATOR);
		result.append(aggregators.get("billing").aggregate(order) + LINE_SEPARATOR);

		for (LineItem lineItem : order.getLineItems()) {
			result.append(aggregators.get("item").aggregate(lineItem) + LINE_SEPARATOR);
		}

		result.append(aggregators.get("footer").aggregate(order));

		return result.toString();
	}

	/**
	 * Set aggregators for all types of lines in the output file
	 * 
	 * @param aggregators
	 */
	public void setAggregators(Map<String, LineAggregator<Object>> aggregators) {
		this.aggregators = aggregators;
	}

}
