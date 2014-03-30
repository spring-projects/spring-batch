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

import java.math.BigDecimal;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.sample.domain.order.internal.mapper.OrderItemFieldSetMapper;
import org.springframework.batch.sample.support.AbstractFieldSetMapperTests;

public class OrderItemFieldSetMapperTests extends AbstractFieldSetMapperTests {
	private static final BigDecimal DISCOUNT_AMOUNT = new BigDecimal("1");
	private static final BigDecimal DISCOUNT_PERC = new BigDecimal("2");
	private static final BigDecimal HANDLING_PRICE = new BigDecimal("3");
	private static final long ITEM_ID = 4;
	private static final BigDecimal PRICE = new BigDecimal("5");
	private static final int QUANTITY = 6;
	private static final BigDecimal SHIPPING_PRICE = new BigDecimal("7");
	private static final BigDecimal TOTAL_PRICE = new BigDecimal("8");

	@Override
	protected Object expectedDomainObject() {
		LineItem item = new LineItem();
		item.setDiscountAmount(DISCOUNT_AMOUNT);
		item.setDiscountPerc(DISCOUNT_PERC);
		item.setHandlingPrice(HANDLING_PRICE);
		item.setItemId(ITEM_ID);
		item.setPrice(PRICE);
		item.setQuantity(QUANTITY);
		item.setShippingPrice(SHIPPING_PRICE);
		item.setTotalPrice(TOTAL_PRICE);
		return item;
	}

	@Override
	protected FieldSet fieldSet() {
		String[] tokens = new String[] { String.valueOf(DISCOUNT_AMOUNT), String.valueOf(DISCOUNT_PERC),
				String.valueOf(HANDLING_PRICE), String.valueOf(ITEM_ID), String.valueOf(PRICE),
				String.valueOf(QUANTITY), String.valueOf(SHIPPING_PRICE), String.valueOf(TOTAL_PRICE) };
		String[] columnNames = new String[] { OrderItemFieldSetMapper.DISCOUNT_AMOUNT_COLUMN,
				OrderItemFieldSetMapper.DISCOUNT_PERC_COLUMN, OrderItemFieldSetMapper.HANDLING_PRICE_COLUMN,
				OrderItemFieldSetMapper.ITEM_ID_COLUMN, OrderItemFieldSetMapper.PRICE_COLUMN,
				OrderItemFieldSetMapper.QUANTITY_COLUMN, OrderItemFieldSetMapper.SHIPPING_PRICE_COLUMN,
				OrderItemFieldSetMapper.TOTAL_PRICE_COLUMN };
		return new DefaultFieldSet(tokens, columnNames);
	}

	@Override
	protected FieldSetMapper<LineItem> fieldSetMapper() {
		return new OrderItemFieldSetMapper();
	}
}
