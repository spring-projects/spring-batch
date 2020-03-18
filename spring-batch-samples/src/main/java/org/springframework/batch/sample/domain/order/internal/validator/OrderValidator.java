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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.batch.sample.domain.order.Address;
import org.springframework.batch.sample.domain.order.BillingInfo;
import org.springframework.batch.sample.domain.order.Customer;
import org.springframework.batch.sample.domain.order.LineItem;
import org.springframework.batch.sample.domain.order.Order;
import org.springframework.batch.sample.domain.order.ShippingInfo;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class OrderValidator implements Validator {

	private static final List<String> CARD_TYPES = new ArrayList<>();
	private static final List<String> SHIPPER_IDS = new ArrayList<>();
	private static final List<String> SHIPPER_TYPES = new ArrayList<>();
	private static final long MAX_ID = 9999999999L;
	private static final BigDecimal BD_MIN = new BigDecimal("0.0");
	private static final BigDecimal BD_MAX = new BigDecimal("99999999.99");
	private static final BigDecimal BD_PERC_MAX = new BigDecimal("100.0");
	private static final int MAX_QUANTITY = 9999;
	private static final BigDecimal BD_100 = new BigDecimal("100.00");

	static {
		CARD_TYPES.add("VISA");
		CARD_TYPES.add("AMEX");
		CARD_TYPES.add("ECMC");
		CARD_TYPES.add("DCIN");
		CARD_TYPES.add("PAYP");

		SHIPPER_IDS.add("FEDX");
		SHIPPER_IDS.add("UPS");
		SHIPPER_IDS.add("DHL");
		SHIPPER_IDS.add("DPD");

		SHIPPER_TYPES.add("STD");
		SHIPPER_TYPES.add("EXP");
		SHIPPER_TYPES.add("AMS");
		SHIPPER_TYPES.add("AME");
	}

	@Override
	public boolean supports(Class<?> arg0) {
		return arg0.isAssignableFrom(Order.class);
	}

	@Override
	public void validate(Object arg0, Errors errors) {
		Order item = null;
		try {
			item = (Order) arg0;
		} catch (ClassCastException cce) {
			errors.reject("Incorrect type");
		}

		if(item != null) {
			validateOrder(item, errors);
			validateCustomer(item.getCustomer(), errors);
			validateAddress(item.getBillingAddress(), errors, "billingAddress");
			validateAddress(item.getShippingAddress(), errors, "shippingAddress");
			validatePayment(item.getBilling(), errors);
			validateShipping(item.getShipping(), errors);
			validateLineItems(item.getLineItems(), errors);
		}
	}

	protected void validateLineItems(List<LineItem> lineItems, Errors errors) {
		boolean ids = true;
		boolean prices = true;
		boolean discounts = true;
		boolean shippingPrices = true;
		boolean handlingPrices = true;
		boolean quantities = true;
		boolean totalPrices = true;

		for (LineItem lineItem : lineItems) {
			if(lineItem.getItemId() <= 0 || lineItem.getItemId() > MAX_ID) {
				ids = false;
			}

			if((BD_MIN.compareTo(lineItem.getPrice()) > 0) || (BD_MAX.compareTo(lineItem.getPrice()) < 0)) {
				prices = false;
			}

			if (BD_MIN.compareTo(lineItem.getDiscountPerc()) != 0) {
				//DiscountPerc must be between 0.0 and 100.0
				if ((BD_MIN.compareTo(lineItem.getDiscountPerc()) > 0)
						|| (BD_PERC_MAX.compareTo(lineItem.getDiscountPerc()) < 0)
						|| (BD_MIN.compareTo(lineItem.getDiscountAmount()) != 0)) { //only one of DiscountAmount and DiscountPerc should be non-zero
					discounts = false;
				}
			} else {
				//DiscountAmount must be between 0.0 and item.price
				if ((BD_MIN.compareTo(lineItem.getDiscountAmount()) > 0)
						|| (lineItem.getPrice().compareTo(lineItem.getDiscountAmount()) < 0)) {
					discounts = false;
				}
			}

			if ((BD_MIN.compareTo(lineItem.getShippingPrice()) > 0) || (BD_MAX.compareTo(lineItem.getShippingPrice()) < 0)) {
				shippingPrices = false;
			}

			if ((BD_MIN.compareTo(lineItem.getHandlingPrice()) > 0) || (BD_MAX.compareTo(lineItem.getHandlingPrice()) < 0)) {
				handlingPrices = false;
			}

			if ((lineItem.getQuantity() <= 0) || (lineItem.getQuantity() > MAX_QUANTITY)) {
				quantities = false;
			}


			if ((BD_MIN.compareTo(lineItem.getTotalPrice()) > 0)
					|| (BD_MAX.compareTo(lineItem.getTotalPrice()) < 0)) {
				totalPrices = false;
			}

			//calculate total price

			//discount coefficient = (100.00 - discountPerc) / 100.00
			BigDecimal coef = BD_100.subtract(lineItem.getDiscountPerc())
					.divide(BD_100, 4, BigDecimal.ROUND_HALF_UP);

			//discountedPrice = (price * coefficient) - discountAmount
			//at least one of discountPerc and discountAmount is 0 - this is validated by ValidateDiscountsFunction
			BigDecimal discountedPrice = lineItem.getPrice().multiply(coef)
					.subtract(lineItem.getDiscountAmount());

			//price for single item = discountedPrice + shipping + handling
			BigDecimal singleItemPrice = discountedPrice.add(lineItem.getShippingPrice())
					.add(lineItem.getHandlingPrice());

			//total price = singleItemPrice * quantity
			BigDecimal quantity = new BigDecimal(lineItem.getQuantity());
			BigDecimal totalPrice = singleItemPrice.multiply(quantity)
					.setScale(2, BigDecimal.ROUND_HALF_UP);

			//calculatedPrice should equal to item.totalPrice
			if (totalPrice.compareTo(lineItem.getTotalPrice()) != 0) {
				totalPrices = false;
			}
		}

		String lineItemsFieldName = "lineItems";

		if(!ids) {
			errors.rejectValue(lineItemsFieldName, "error.lineitems.id");
		}

		if(!prices) {
			errors.rejectValue(lineItemsFieldName, "error.lineitems.price");
		}

		if(!discounts) {
			errors.rejectValue(lineItemsFieldName, "error.lineitems.discount");
		}

		if(!shippingPrices) {
			errors.rejectValue(lineItemsFieldName, "error.lineitems.shipping");
		}

		if(!handlingPrices) {
			errors.rejectValue(lineItemsFieldName, "error.lineitems.handling");
		}

		if(!quantities) {
			errors.rejectValue(lineItemsFieldName, "error.lineitems.quantity");
		}

		if(!totalPrices) {
			errors.rejectValue(lineItemsFieldName, "error.lineitems.totalprice");
		}
	}

	protected void validateShipping(ShippingInfo shipping, Errors errors) {
		if(!SHIPPER_IDS.contains(shipping.getShipperId())) {
			errors.rejectValue("shipping.shipperId", "error.shipping.shipper");
		}

		if(!SHIPPER_TYPES.contains(shipping.getShippingTypeId())) {
			errors.rejectValue("shipping.shippingTypeId", "error.shipping.type");
		}

		if(StringUtils.hasText(shipping.getShippingInfo())) {
			validateStringLength(shipping.getShippingInfo(), errors, "shipping.shippingInfo", "error.shipping.shippinginfo.length", 100);
		}
	}

	protected void validatePayment(BillingInfo billing, Errors errors) {
		if(!CARD_TYPES.contains(billing.getPaymentId())) {
			errors.rejectValue("billing.paymentId", "error.billing.type");
		}

		if(!billing.getPaymentDesc().matches("[A-Z]{4}-[0-9]{10,11}")) {
			errors.rejectValue("billing.paymentDesc", "error.billing.desc");
		}
	}

	protected void validateAddress(Address address, Errors errors,
			String prefix) {
		if(address != null) {
			if(StringUtils.hasText(address.getAddressee())) {
				validateStringLength(address.getAddressee(), errors, prefix + ".addressee", "error.baddress.addresse.length", 60);
			}

			validateStringLength(address.getAddrLine1(), errors, prefix + ".addrLine1", "error.baddress.addrline1.length", 50);

			if(StringUtils.hasText(address.getAddrLine2())) {
				validateStringLength(address.getAddrLine2(), errors, prefix + ".addrLine2", "error.baddress.addrline2.length", 50);
			}
			validateStringLength(address.getCity(), errors, prefix + ".city", "error.baddress.city.length", 30);
			validateStringLength(address.getZipCode(), errors, prefix + ".zipCode", "error.baddress.zipcode.length", 5);

			if(StringUtils.hasText(address.getZipCode()) && !address.getZipCode().matches("[0-9]{5}")) {
				errors.rejectValue(prefix + ".zipCode", "error.baddress.zipcode.format");
			}

			if((!StringUtils.hasText(address.getState()) && ("United States".equals(address.getCountry())) || StringUtils.hasText(address.getState()) && address.getState().length() != 2)) {
				errors.rejectValue(prefix + ".state", "error.baddress.state.length");
			}

			validateStringLength(address.getCountry(), errors, prefix + ".country", "error.baddress.country.length", 50);
		}
	}

	protected void validateStringLength(String string, Errors errors,
			String field, String message, int length) {
		if(!StringUtils.hasText(string) || string.length() > length) {
			errors.rejectValue(field, message);
		}
	}

	protected void validateCustomer(Customer customer, Errors errors) {
		if(!customer.isRegistered() && customer.isBusinessCustomer()) {
			errors.rejectValue("customer.registered", "error.customer.registration");
		}

		if(!StringUtils.hasText(customer.getCompanyName()) && customer.isBusinessCustomer()) {
			errors.rejectValue("customer.companyName", "error.customer.companyname");
		}

		if(!StringUtils.hasText(customer.getFirstName()) && !customer.isBusinessCustomer()) {
			errors.rejectValue("customer.firstName", "error.customer.firstname");
		}

		if(!StringUtils.hasText(customer.getLastName()) && !customer.isBusinessCustomer()) {
			errors.rejectValue("customer.lastName", "error.customer.lastname");
		}

		if(customer.isRegistered() && (customer.getRegistrationId() < 0 || customer.getRegistrationId() >= 99999999L)) {
			errors.rejectValue("customer.registrationId", "error.customer.registrationid");
		}
	}

	protected void validateOrder(Order item, Errors errors) {
		if(item.getOrderId() < 0 || item.getOrderId() > 9999999999L) {
			errors.rejectValue("orderId", "error.order.id");
		}

		if(new Date().compareTo(item.getOrderDate()) < 0) {
			errors.rejectValue("orderDate", "error.order.date.future");
		}

		if(item.getLineItems() != null && item.getTotalLines() != item.getLineItems().size()) {
			errors.rejectValue("totalLines", "error.order.lines.badcount");
		}
	}
}
