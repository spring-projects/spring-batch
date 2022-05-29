/*
 * Copyright 2006-2014 the original author or authors.
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

package org.springframework.batch.sample.domain.order.internal.xml;

import java.util.Date;
import java.util.List;

/**
 * An XML order.
 *
 * This is a complex type.
 */
public class Order {

	private Customer customer;

	private Date date;

	private List<LineItem> lineItems;

	private Shipper shipper;

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public Date getDate() {
		return date != null ? new Date(date.getTime()) : null;
	}

	public void setDate(Date date) {
		this.date = date != null ? new Date(date.getTime()) : null;
	}

	public List<LineItem> getLineItems() {
		return lineItems;
	}

	public void setLineItems(List<LineItem> lineItems) {
		this.lineItems = lineItems;
	}

	public Shipper getShipper() {
		return shipper;
	}

	public void setShipper(Shipper shipper) {
		this.shipper = shipper;
	}

	@Override
	public String toString() {
		return "Order [customer=" + customer + ", date=" + date + ", lineItems=" + lineItems + "]";
	}

}
