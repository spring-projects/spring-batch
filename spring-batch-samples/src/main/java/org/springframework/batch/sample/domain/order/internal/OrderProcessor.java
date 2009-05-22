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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.file.transform.LineAggregator;
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
	private Map<String, LineAggregator<Object>> aggregators;

	/**
	 * Converts information from an Order object to a collection of Strings for
	 * output.
	 * 
	 * @throws Exception
	 */
	public List<String> process(Order order) throws Exception {

		List<String> result = new ArrayList<String>();

		result.add(aggregators.get("header").aggregate(order));
		result.add(aggregators.get("customer").aggregate(order));
		result.add(aggregators.get("address").aggregate(order));
		result.add(aggregators.get("billing").aggregate(order));

		for (LineItem lineItem : order.getLineItems()) {
			result.add(aggregators.get("item").aggregate(lineItem));
		}

		result.add(aggregators.get("footer").aggregate(order));

		return result;
	}

	public void setAggregators(Map<String, LineAggregator<Object>> aggregators) {
		this.aggregators = aggregators;
	}

}
