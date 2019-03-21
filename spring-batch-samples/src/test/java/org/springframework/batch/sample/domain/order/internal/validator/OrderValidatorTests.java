/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.batch.sample.domain.order.internal.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.sample.domain.order.Address;
import org.springframework.batch.sample.domain.order.BillingInfo;
import org.springframework.batch.sample.domain.order.Customer;
import org.springframework.batch.sample.domain.order.LineItem;
import org.springframework.batch.sample.domain.order.Order;
import org.springframework.batch.sample.domain.order.ShippingInfo;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

public class OrderValidatorTests {

	private OrderValidator orderValidator;

	@Before
	public void setUp() throws Exception {
		orderValidator = new OrderValidator();
	}

	@Test
	public void testSupports() {
		assertTrue(orderValidator.supports(Order.class));
	}

	@Test
	public void testNotAnOrder() {
		String notAnOrder = "order";
		Errors errors = new BeanPropertyBindingResult(notAnOrder, "validOrder");

		orderValidator.validate(notAnOrder, errors);

		assertEquals(1, errors.getAllErrors().size());
		assertEquals("Incorrect type", errors.getAllErrors().get(0).getCode());

		errors = new BeanPropertyBindingResult(notAnOrder, "validOrder");

		orderValidator.validate(null, errors);
		assertEquals(0, errors.getAllErrors().size());
	}

	@Test
	public void testValidOrder() {
		Order order = new Order();
		order.setOrderId(-5);
		order.setOrderDate(new Date(new Date().getTime() + 1000000000L));
		order.setTotalLines(10);
		order.setLineItems(new ArrayList<>());

		Errors errors = new BeanPropertyBindingResult(order, "validOrder");

		orderValidator.validateOrder(order, errors);

		assertEquals(3, errors.getAllErrors().size());
		assertEquals("error.order.id", errors.getFieldError("orderId").getCode());
		assertEquals("error.order.date.future", errors.getFieldError("orderDate").getCode());
		assertEquals("error.order.lines.badcount", errors.getFieldError("totalLines").getCode());

		order = new Order();
		order.setOrderId(Long.MAX_VALUE);
		order.setOrderDate(new Date(new Date().getTime() - 1000));
		order.setTotalLines(0);
		List<LineItem> items = new ArrayList<>();
		items.add(new LineItem());
		order.setLineItems(items);

		errors = new BeanPropertyBindingResult(order, "validOrder");

		orderValidator.validateOrder(order, errors);

		assertEquals(2, errors.getAllErrors().size());
		assertEquals("error.order.id", errors.getFieldError("orderId").getCode());
		assertEquals("error.order.lines.badcount", errors.getFieldError("totalLines").getCode());

		order = new Order();
		order.setOrderId(5L);
		order.setOrderDate(new Date(new Date().getTime() - 1000));
		order.setTotalLines(1);
		items = new ArrayList<>();
		items.add(new LineItem());
		order.setLineItems(items);

		errors = new BeanPropertyBindingResult(order, "validOrder");

		orderValidator.validateOrder(order, errors);

		assertEquals(0, errors.getAllErrors().size());
	}

	@Test
	public void testValidCustomer() {
		Order order = new Order();
		Customer customer = new Customer();
		customer.setRegistered(false);
		customer.setBusinessCustomer(true);
		order.setCustomer(customer);

		Errors errors = new BeanPropertyBindingResult(order, "validOrder");
		orderValidator.validateCustomer(customer, errors);

		assertEquals(2, errors.getAllErrors().size());
		assertEquals("error.customer.registration", errors.getFieldError("customer.registered").getCode());
		assertEquals("error.customer.companyname", errors.getFieldError("customer.companyName").getCode());

		customer = new Customer();
		customer.setRegistered(true);
		customer.setBusinessCustomer(false);
		customer.setRegistrationId(Long.MIN_VALUE);
		order.setCustomer(customer);

		errors = new BeanPropertyBindingResult(order, "validOrder");
		orderValidator.validateCustomer(customer, errors);

		assertEquals(3, errors.getAllErrors().size());
		assertEquals("error.customer.firstname", errors.getFieldError("customer.firstName").getCode());
		assertEquals("error.customer.lastname", errors.getFieldError("customer.lastName").getCode());
		assertEquals("error.customer.registrationid", errors.getFieldError("customer.registrationId").getCode());

		customer = new Customer();
		customer.setRegistered(true);
		customer.setBusinessCustomer(false);
		customer.setRegistrationId(Long.MAX_VALUE);
		order.setCustomer(customer);

		errors = new BeanPropertyBindingResult(order, "validOrder");
		orderValidator.validateCustomer(customer, errors);

		assertEquals(3, errors.getAllErrors().size());
		assertEquals("error.customer.firstname", errors.getFieldError("customer.firstName").getCode());
		assertEquals("error.customer.lastname", errors.getFieldError("customer.lastName").getCode());
		assertEquals("error.customer.registrationid", errors.getFieldError("customer.registrationId").getCode());

		customer = new Customer();
		customer.setRegistered(true);
		customer.setBusinessCustomer(true);
		customer.setCompanyName("Acme Inc");
		customer.setRegistrationId(5L);
		order.setCustomer(customer);

		errors = new BeanPropertyBindingResult(order, "validOrder");
		orderValidator.validateCustomer(customer, errors);

		assertEquals(0, errors.getAllErrors().size());

		customer = new Customer();
		customer.setRegistered(true);
		customer.setBusinessCustomer(false);
		customer.setFirstName("John");
		customer.setLastName("Doe");
		customer.setRegistrationId(5L);
		order.setCustomer(customer);

		errors = new BeanPropertyBindingResult(order, "validOrder");
		orderValidator.validateCustomer(customer, errors);

		assertEquals(0, errors.getAllErrors().size());
	}

	@Test
	public void testValidAddress() {
		Order order = new Order();

		Errors errors = new BeanPropertyBindingResult(order, "validOrder");
		orderValidator.validateAddress(null, errors, "billingAddress");

		assertEquals(0, errors.getAllErrors().size());

		Address address = new Address();
		order.setBillingAddress(address);

		orderValidator.validateAddress(address, errors, "billingAddress");

		assertEquals(4, errors.getAllErrors().size());
		assertEquals("error.baddress.addrline1.length", errors.getFieldError("billingAddress.addrLine1").getCode());
		assertEquals("error.baddress.city.length", errors.getFieldError("billingAddress.city").getCode());
		assertEquals("error.baddress.zipcode.length", errors.getFieldError("billingAddress.zipCode").getCode());
		assertEquals("error.baddress.country.length", errors.getFieldError("billingAddress.country").getCode());

		address = new Address();
		address.setAddressee("1234567890123456789012345678901234567890123456789012345678901234567890");
		address.setAddrLine1("123456789012345678901234567890123456789012345678901234567890");
		address.setAddrLine2("123456789012345678901234567890123456789012345678901234567890");
		address.setCity("1234567890123456789012345678901234567890");
		address.setZipCode("1234567890");
		address.setState("1234567890");
		address.setCountry("123456789012345678901234567890123456789012345678901234567890");
		order.setBillingAddress(address);

		errors = new BeanPropertyBindingResult(order, "validOrder");
		orderValidator.validateAddress(address, errors, "billingAddress");

		assertEquals(8, errors.getAllErrors().size());
		assertEquals("error.baddress.addresse.length", errors.getFieldError("billingAddress.addressee").getCode());
		assertEquals("error.baddress.addrline1.length", errors.getFieldError("billingAddress.addrLine1").getCode());
		assertEquals("error.baddress.addrline2.length", errors.getFieldError("billingAddress.addrLine2").getCode());
		assertEquals("error.baddress.city.length", errors.getFieldError("billingAddress.city").getCode());
		assertEquals("error.baddress.state.length", errors.getFieldError("billingAddress.state").getCode());
		assertEquals("error.baddress.zipcode.length", errors.getFieldErrors("billingAddress.zipCode").get(0).getCode());
		assertEquals("error.baddress.zipcode.format", errors.getFieldErrors("billingAddress.zipCode").get(1).getCode());
		assertEquals("error.baddress.country.length", errors.getFieldError("billingAddress.country").getCode());

		address = new Address();
		address.setAddressee("John Doe");
		address.setAddrLine1("123 4th Street");
		address.setCity("Chicago");
		address.setState("IL");
		address.setZipCode("60606");
		address.setCountry("United States");
		order.setBillingAddress(address);

		errors = new BeanPropertyBindingResult(order, "validOrder");
		orderValidator.validateAddress(address, errors, "billingAddress");

		assertEquals(0, errors.getAllErrors().size());
	}

	@Test
	public void testValidPayment() {
		Order order = new Order();
		BillingInfo info = new BillingInfo();
		info.setPaymentId("INVALID");
		info.setPaymentDesc("INVALID");
		order.setBilling(info);

		Errors errors = new BeanPropertyBindingResult(order, "validOrder");
		orderValidator.validatePayment(info, errors);

		assertEquals(2, errors.getAllErrors().size());
		assertEquals("error.billing.type", errors.getFieldError("billing.paymentId").getCode());
		assertEquals("error.billing.desc", errors.getFieldError("billing.paymentDesc").getCode());

		info = new BillingInfo();
		info.setPaymentId("VISA");
		info.setPaymentDesc("ADFI-1234567890");
		order.setBilling(info);

		errors = new BeanPropertyBindingResult(order, "validOrder");
		orderValidator.validatePayment(info, errors);

		assertEquals(0, errors.getAllErrors().size());
	}

	@Test
	public void testValidShipping() {
		Order order = new Order();
		ShippingInfo info = new ShippingInfo();
		info.setShipperId("INVALID");
		info.setShippingTypeId("INVALID");
		order.setShipping(info);

		Errors errors = new BeanPropertyBindingResult(order, "validOrder");
		orderValidator.validateShipping(info, errors);

		assertEquals(2, errors.getAllErrors().size());
		assertEquals("error.shipping.shipper", errors.getFieldError("shipping.shipperId").getCode());
		assertEquals("error.shipping.type", errors.getFieldError("shipping.shippingTypeId").getCode());

		info = new ShippingInfo();
		info.setShipperId("FEDX");
		info.setShippingTypeId("EXP");
		info.setShippingInfo("12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890");
		order.setShipping(info);

		errors = new BeanPropertyBindingResult(order, "validOrder");
		orderValidator.validateShipping(info, errors);

		assertEquals(1, errors.getAllErrors().size());
		assertEquals("error.shipping.shippinginfo.length", errors.getFieldError("shipping.shippingInfo").getCode());

		info = new ShippingInfo();
		info.setShipperId("FEDX");
		info.setShippingTypeId("EXP");
		info.setShippingInfo("Info");
		order.setShipping(info);

		errors = new BeanPropertyBindingResult(order, "validOrder");
		orderValidator.validateShipping(info, errors);

		assertEquals(0, errors.getAllErrors().size());
	}

	@Test
	public void testValidLineItems() {
		Order order = new Order();
		List<LineItem> lineItems = new ArrayList<>();
		lineItems.add(buildLineItem(-5, 5.00, 0, 0, 2, 3, 3, 30));
		lineItems.add(buildLineItem(Long.MAX_VALUE, 5.00, 0, 0, 2, 3, 3, 30));
		lineItems.add(buildLineItem(6, -5.00, 0, 0, 2, 3, 3, 0));
		lineItems.add(buildLineItem(6, Integer.MAX_VALUE, 0, 0, 2, 3, 3, 30));
		lineItems.add(buildLineItem(6, 5.00, 900, 0, 2, 3, 3, 30));
		lineItems.add(buildLineItem(6, 5.00, -90, 0, 2, 3, 3, 30));
		lineItems.add(buildLineItem(6, 5.00, 10, 20, 2, 3, 3, 30));
		lineItems.add(buildLineItem(6, 5.00, 0, -10, 2, 3, 3, 30));
		lineItems.add(buildLineItem(6, 5.00, 0, 50, 2, 3, 3, 30));
		lineItems.add(buildLineItem(6, 5.00, 0, 0, -2, 3, 3, 30));
		lineItems.add(buildLineItem(6, 5.00, 0, 0, Long.MAX_VALUE, 3, 3, 30));
		lineItems.add(buildLineItem(6, 5.00, 0, 0, 2, -3, 3, 30));
		lineItems.add(buildLineItem(6, 5.00, 0, 0, 2, Long.MAX_VALUE, 3, 30));
		lineItems.add(buildLineItem(6, 5.00, 0, 0, 2, 3, -3, 30));
		lineItems.add(buildLineItem(6, 5.00, 0, 0, 2, 3, Integer.MAX_VALUE, 30));
		lineItems.add(buildLineItem(6, 5.00, 0, 0, 2, 3, 3, -5));
		lineItems.add(buildLineItem(6, 5.00, 0, 0, 2, 3, 3, Integer.MAX_VALUE));
		order.setLineItems(lineItems);

		Errors errors = new BeanPropertyBindingResult(order, "validOrder");
		orderValidator.validateLineItems(lineItems, errors);

		assertEquals(7, errors.getAllErrors().size());
		assertEquals("error.lineitems.id", errors.getFieldErrors("lineItems").get(0).getCode());
		assertEquals("error.lineitems.price", errors.getFieldErrors("lineItems").get(1).getCode());
		assertEquals("error.lineitems.discount", errors.getFieldErrors("lineItems").get(2).getCode());
		assertEquals("error.lineitems.shipping", errors.getFieldErrors("lineItems").get(3).getCode());
		assertEquals("error.lineitems.handling", errors.getFieldErrors("lineItems").get(4).getCode());
		assertEquals("error.lineitems.quantity", errors.getFieldErrors("lineItems").get(5).getCode());
		assertEquals("error.lineitems.totalprice", errors.getFieldErrors("lineItems").get(6).getCode());
	}

	private LineItem buildLineItem(long itemId, double price, int discountPercentage, int discountAmount, long shippingPrice, long handlingPrice, int qty, int totalPrice) {
		LineItem invalidId = new LineItem();
		invalidId.setItemId(itemId);
		invalidId.setPrice(new BigDecimal(price));
		invalidId.setDiscountPerc(new BigDecimal(discountPercentage));
		invalidId.setDiscountAmount(new BigDecimal(discountAmount));
		invalidId.setShippingPrice(new BigDecimal(shippingPrice));
		invalidId.setHandlingPrice(new BigDecimal(handlingPrice));
		invalidId.setQuantity(qty);
		invalidId.setTotalPrice(new BigDecimal(totalPrice));
		return invalidId;
	}
}
