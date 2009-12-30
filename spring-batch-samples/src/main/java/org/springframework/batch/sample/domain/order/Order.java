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

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class Order {
	public static final String LINE_ID_HEADER = "HEA";

	public static final String LINE_ID_FOOTER = "FOT";

	// header
	private long orderId;

	private Date orderDate;

	// footer
	private int totalLines;

	private int totalItems;

	private BigDecimal totalPrice;

	private Customer customer;

	private Address billingAddress;

	private Address shippingAddress;

	private BillingInfo billing;

	private ShippingInfo shipping;

	// order items
	private List<LineItem> lineItems;

	public BillingInfo getBilling() {
		return billing;
	}

	public void setBilling(BillingInfo billing) {
		this.billing = billing;
	}

	public Address getBillingAddress() {
		return billingAddress;
	}

	public void setBillingAddress(Address billingAddress) {
		this.billingAddress = billingAddress;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public List<LineItem> getLineItems() {
		return lineItems;
	}

	public void setLineItems(List<LineItem> lineItems) {
		this.lineItems = lineItems;
	}

	public Date getOrderDate() {
		return orderDate;
	}

	public void setOrderDate(Date orderDate) {
		this.orderDate = orderDate;
	}

	public long getOrderId() {
		return orderId;
	}

	public void setOrderId(long orderId) {
		this.orderId = orderId;
	}

	public ShippingInfo getShipping() {
		return shipping;
	}

	public void setShipping(ShippingInfo shipping) {
		this.shipping = shipping;
	}

	public Address getShippingAddress() {
		return shippingAddress;
	}

	public void setShippingAddress(Address shippingAddress) {
		this.shippingAddress = shippingAddress;
	}

	public BigDecimal getTotalPrice() {
		return totalPrice;
	}

	public void setTotalPrice(BigDecimal totalPrice) {
		this.totalPrice = totalPrice;
	}

	public int getTotalItems() {
		return totalItems;
	}

	public void setTotalItems(int totalItems) {
		this.totalItems = totalItems;
	}

	public int getTotalLines() {
		return totalLines;
	}

	public void setTotalLines(int totalLines) {
		this.totalLines = totalLines;
	}

	@Override
	public String toString() {
		return "Order [customer=" + customer + ", orderId=" + orderId + ", totalItems=" + totalItems + ", totalPrice="
				+ totalPrice + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((billing == null) ? 0 : billing.hashCode());
		result = prime * result + ((billingAddress == null) ? 0 : billingAddress.hashCode());
		result = prime * result + ((customer == null) ? 0 : customer.hashCode());
		result = prime * result + ((lineItems == null) ? 0 : lineItems.hashCode());
		result = prime * result + ((orderDate == null) ? 0 : orderDate.hashCode());
		result = prime * result + (int) (orderId ^ (orderId >>> 32));
		result = prime * result + ((shipping == null) ? 0 : shipping.hashCode());
		result = prime * result + ((shippingAddress == null) ? 0 : shippingAddress.hashCode());
		result = prime * result + totalItems;
		result = prime * result + totalLines;
		result = prime * result + ((totalPrice == null) ? 0 : totalPrice.hashCode());
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
		if (billing == null) {
			if (other.billing != null)
				return false;
		}
		else if (!billing.equals(other.billing))
			return false;
		if (billingAddress == null) {
			if (other.billingAddress != null)
				return false;
		}
		else if (!billingAddress.equals(other.billingAddress))
			return false;
		if (customer == null) {
			if (other.customer != null)
				return false;
		}
		else if (!customer.equals(other.customer))
			return false;
		if (lineItems == null) {
			if (other.lineItems != null)
				return false;
		}
		else if (!lineItems.equals(other.lineItems))
			return false;
		if (orderDate == null) {
			if (other.orderDate != null)
				return false;
		}
		else if (!orderDate.equals(other.orderDate))
			return false;
		if (orderId != other.orderId)
			return false;
		if (shipping == null) {
			if (other.shipping != null)
				return false;
		}
		else if (!shipping.equals(other.shipping))
			return false;
		if (shippingAddress == null) {
			if (other.shippingAddress != null)
				return false;
		}
		else if (!shippingAddress.equals(other.shippingAddress))
			return false;
		if (totalItems != other.totalItems)
			return false;
		if (totalLines != other.totalLines)
			return false;
		if (totalPrice == null) {
			if (other.totalPrice != null)
				return false;
		}
		else if (!totalPrice.equals(other.totalPrice))
			return false;
		return true;
	}

}
