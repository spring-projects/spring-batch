/*
 * Copyright 2008-2014 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.sample.domain.order.internal.OrderItemReader;

public class OrderItemReaderTests {
	private OrderItemReader provider;
	private ItemReader<FieldSet> input;

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() {
		input = mock(ItemReader.class);

		provider = new OrderItemReader();
		provider.setFieldSetReader(input);
	}

	/*
	 * OrderItemProvider is responsible for retrieving validated value object
	 * from input source. OrderItemProvider.next(): - reads lines from the input
	 * source - returned as fieldsets - pass fieldsets to the mapper - mapper
	 * will create value object - pass value object to validator - returns
	 * validated object
	 * 
	 * In testNext method we are going to test these responsibilities. So we
	 * need create mock objects for input source, mapper and validator.
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testNext() throws Exception {
		FieldSet headerFS = new DefaultFieldSet(new String[] { Order.LINE_ID_HEADER });
		FieldSet customerFS = new DefaultFieldSet(new String[] { Customer.LINE_ID_NON_BUSINESS_CUST });
		FieldSet billingFS = new DefaultFieldSet(new String[] { Address.LINE_ID_BILLING_ADDR });
		FieldSet shippingFS = new DefaultFieldSet(new String[] { Address.LINE_ID_SHIPPING_ADDR });
		FieldSet billingInfoFS = new DefaultFieldSet(new String[] { BillingInfo.LINE_ID_BILLING_INFO });
		FieldSet shippingInfoFS = new DefaultFieldSet(new String[] { ShippingInfo.LINE_ID_SHIPPING_INFO });
		FieldSet itemFS = new DefaultFieldSet(new String[] { LineItem.LINE_ID_ITEM });
		FieldSet footerFS = new DefaultFieldSet(new String[] { Order.LINE_ID_FOOTER, "100", "3", "3" }, new String[] {
				"ID", "TOTAL_PRICE", "TOTAL_LINE_ITEMS", "TOTAL_ITEMS" });

		when(input.read()).thenReturn(headerFS, customerFS, billingFS, shippingFS, billingInfoFS,
				shippingInfoFS, itemFS, itemFS, itemFS, footerFS, null);

		Order order = new Order();
		Customer customer = new Customer();
		Address billing = new Address();
		Address shipping = new Address();
		BillingInfo billingInfo = new BillingInfo();
		ShippingInfo shippingInfo = new ShippingInfo();
		LineItem item = new LineItem();

		@SuppressWarnings("rawtypes")
		FieldSetMapper mapper = mock(FieldSetMapper.class);
		when(mapper.mapFieldSet(headerFS)).thenReturn(order);
		when(mapper.mapFieldSet(customerFS)).thenReturn(customer);
		when(mapper.mapFieldSet(billingFS)).thenReturn(billing);
		when(mapper.mapFieldSet(shippingFS)).thenReturn(shipping);
		when(mapper.mapFieldSet(billingInfoFS)).thenReturn(billingInfo);
		when(mapper.mapFieldSet(shippingInfoFS)).thenReturn(shippingInfo);
		when(mapper.mapFieldSet(itemFS)).thenReturn(item);

		provider.setAddressMapper(mapper);
		provider.setBillingMapper(mapper);
		provider.setCustomerMapper(mapper);
		provider.setHeaderMapper(mapper);
		provider.setItemMapper(mapper);
		provider.setShippingMapper(mapper);

		Object result = provider.read();

		assertNotNull(result);

		Order o = (Order) result;
		assertEquals(o, order);
		assertEquals(o.getCustomer(), customer);
		assertFalse(o.getCustomer().isBusinessCustomer());
		assertEquals(o.getBillingAddress(), billing);
		assertEquals(o.getShippingAddress(), shipping);
		assertEquals(o.getBilling(), billingInfo);
		assertEquals(o.getShipping(), shippingInfo);

		assertEquals(3, o.getLineItems().size());

		for (LineItem lineItem : o.getLineItems()) {
			assertEquals(lineItem, item);
		}

		assertNull(provider.read());
	}
}
