/*
 * Copyright 2006-2023 the original author or authors.
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

package org.springframework.batch.samples.file.patternmatching.internal;

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.file.mapping.FieldSetMapper;
import org.springframework.batch.infrastructure.item.file.transform.FieldSet;
import org.springframework.batch.samples.file.patternmatching.Address;
import org.springframework.batch.samples.file.patternmatching.BillingInfo;
import org.springframework.batch.samples.file.patternmatching.Customer;
import org.springframework.batch.samples.file.patternmatching.LineItem;
import org.springframework.batch.samples.file.patternmatching.Order;
import org.springframework.batch.samples.file.patternmatching.ShippingInfo;

/**
 * @author peter.zozom
 * @author Mahmoud Ben Hassine
 *
 */
public class OrderItemReader implements ItemReader<Order> {

	private static final Log log = LogFactory.getLog(OrderItemReader.class);

	private Order order;

	private boolean recordFinished;

	private FieldSetMapper<Order> headerMapper;

	private FieldSetMapper<Customer> customerMapper;

	private FieldSetMapper<Address> addressMapper;

	private FieldSetMapper<BillingInfo> billingMapper;

	private FieldSetMapper<LineItem> itemMapper;

	private FieldSetMapper<ShippingInfo> shippingMapper;

	private ItemReader<FieldSet> fieldSetReader;

	/**
	 * @see ItemReader#read()
	 */
	@Override
	public @Nullable Order read() throws Exception {
		recordFinished = false;

		while (!recordFinished) {
			process(fieldSetReader.read());
		}

		if (log.isInfoEnabled()) {
			log.info("Mapped: " + order);
		}
		Order result = order;
		order = null;

		return result;
	}

	private void process(FieldSet fieldSet) throws Exception {
		// finish processing if we hit the end of file
		if (fieldSet == null) {
			log.debug("FINISHED");
			recordFinished = true;
			order = null;
			return;
		}

		String lineId = fieldSet.readString(0);

		switch (lineId) {
			case Order.LINE_ID_HEADER -> {
				log.debug("STARTING NEW RECORD");
				order = headerMapper.mapFieldSet(fieldSet);
			}
			case Order.LINE_ID_FOOTER -> {
				log.debug("END OF RECORD");

				// Do mapping for footer here, because mapper does not allow to pass
				// an Order object as input.
				// Mapper always creates new object
				order.setTotalPrice(fieldSet.readBigDecimal("TOTAL_PRICE"));
				order.setTotalLines(fieldSet.readInt("TOTAL_LINE_ITEMS"));
				order.setTotalItems(fieldSet.readInt("TOTAL_ITEMS"));

				// mark we are finished with current Order
				recordFinished = true;
			}
			case Customer.LINE_ID_BUSINESS_CUST -> {
				log.debug("MAPPING CUSTOMER");
				if (order.getCustomer() == null) {
					Customer customer = customerMapper.mapFieldSet(fieldSet);
					customer.setBusinessCustomer(true);
					order.setCustomer(customer);
				}
			}
			case Customer.LINE_ID_NON_BUSINESS_CUST -> {
				log.debug("MAPPING CUSTOMER");
				if (order.getCustomer() == null) {
					Customer customer = customerMapper.mapFieldSet(fieldSet);
					customer.setBusinessCustomer(false);
					order.setCustomer(customer);
				}
			}
			case Address.LINE_ID_BILLING_ADDR -> {
				log.debug("MAPPING BILLING ADDRESS");
				order.setBillingAddress(addressMapper.mapFieldSet(fieldSet));
			}
			case Address.LINE_ID_SHIPPING_ADDR -> {
				log.debug("MAPPING SHIPPING ADDRESS");
				order.setShippingAddress(addressMapper.mapFieldSet(fieldSet));
			}
			case BillingInfo.LINE_ID_BILLING_INFO -> {
				log.debug("MAPPING BILLING INFO");
				order.setBilling(billingMapper.mapFieldSet(fieldSet));
			}
			case ShippingInfo.LINE_ID_SHIPPING_INFO -> {
				log.debug("MAPPING SHIPPING INFO");
				order.setShipping(shippingMapper.mapFieldSet(fieldSet));
			}
			case LineItem.LINE_ID_ITEM -> {
				log.debug("MAPPING LINE ITEM");
				if (order.getLineItems() == null) {
					order.setLineItems(new ArrayList<>());
				}
				order.getLineItems().add(itemMapper.mapFieldSet(fieldSet));
			}
			default -> {
				if (log.isDebugEnabled()) {
					log.debug("Could not map LINE_ID=" + lineId);
				}
			}
		}
	}

	/**
	 * @param fieldSetReader reads lines from the file converting them to
	 * {@link FieldSet}.
	 */
	public void setFieldSetReader(ItemReader<FieldSet> fieldSetReader) {
		this.fieldSetReader = fieldSetReader;
	}

	public void setAddressMapper(FieldSetMapper<Address> addressMapper) {
		this.addressMapper = addressMapper;
	}

	public void setBillingMapper(FieldSetMapper<BillingInfo> billingMapper) {
		this.billingMapper = billingMapper;
	}

	public void setCustomerMapper(FieldSetMapper<Customer> customerMapper) {
		this.customerMapper = customerMapper;
	}

	public void setHeaderMapper(FieldSetMapper<Order> headerMapper) {
		this.headerMapper = headerMapper;
	}

	public void setItemMapper(FieldSetMapper<LineItem> itemMapper) {
		this.itemMapper = itemMapper;
	}

	public void setShippingMapper(FieldSetMapper<ShippingInfo> shippingMapper) {
		this.shippingMapper = shippingMapper;
	}

}
