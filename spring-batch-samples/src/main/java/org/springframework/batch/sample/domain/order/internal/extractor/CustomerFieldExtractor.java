/*
 * Copyright 2009-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.sample.domain.order.internal.extractor;

import org.springframework.batch.item.file.transform.FieldExtractor;
import org.springframework.batch.sample.domain.order.Customer;
import org.springframework.batch.sample.domain.order.Order;

/**
 * @author Dan Garrette
 * @since 2.0.1
 */
public class CustomerFieldExtractor implements FieldExtractor<Order> {

	@Override
	public Object[] extract(Order order) {
		Customer customer = order.getCustomer();
		return new Object[] { "CUSTOMER:", customer.getRegistrationId(), emptyIfNull(customer.getFirstName()),
				emptyIfNull(customer.getMiddleName()), emptyIfNull(customer.getLastName()) };
	}

	private String emptyIfNull(String s) {
		return s != null ? s : "";
	}

}
