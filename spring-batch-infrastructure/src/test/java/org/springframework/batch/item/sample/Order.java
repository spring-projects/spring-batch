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

package org.springframework.batch.item.sample;

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
		return (Date)date.clone();
	}

	public void setDate(Date date) {
		this.date = date == null ? null : (Date)date.clone();
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((customer == null) ? 0 : customer.hashCode());
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		result = prime * result + ((lineItems == null) ? 0 : lineItems.hashCode());
		result = prime * result + ((shipper == null) ? 0 : shipper.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Order other = (Order) obj;
		if (customer == null) {
			if (other.customer != null)
				return false;
		}
		else if (!customer.equals(other.customer))
			return false;
		if (date == null) {
			if (other.date != null)
				return false;
		}
		else if (!date.equals(other.date))
			return false;
		if (lineItems == null) {
			if (other.lineItems != null)
				return false;
		}
		else if (!lineItems.equals(other.lineItems))
			return false;
		if (shipper == null) {
			if (other.shipper != null)
				return false;
		}
		else if (!shipper.equals(other.shipper))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Order [customer=" + customer + ", date=" + date + ", lineItems=" + lineItems + ", shipper=" + shipper
				+ "]";
	}

}
