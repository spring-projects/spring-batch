package org.springframework.batch.sample.domain.order;

import java.math.BigDecimal;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.sample.domain.order.internal.OrderItemFieldSetMapper;
import org.springframework.batch.sample.support.AbstractFieldSetMapperTests;

public class OrderItemFieldSetMapperTests extends AbstractFieldSetMapperTests{

	private static final BigDecimal DISCOUNT_AMOUNT = new BigDecimal("1");
	private static final BigDecimal DISCOUNT_PERC = new BigDecimal("2");
	private static final BigDecimal HANDLING_PRICE = new BigDecimal("3");
	private static final long ITEM_ID = 4;
	private static final BigDecimal PRICE = new BigDecimal("5");
	private static final int QUANTITY = 6;
	private static final BigDecimal SHIPPING_PRICE = new BigDecimal("7");
	private static final BigDecimal TOTAL_PRICE = new BigDecimal("8");

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

	protected FieldSet fieldSet() {
		String[] tokens = new String[]{
				String.valueOf(DISCOUNT_AMOUNT),
				String.valueOf(DISCOUNT_PERC),
				String.valueOf(HANDLING_PRICE),
				String.valueOf(ITEM_ID),
				String.valueOf(PRICE),
				String.valueOf(QUANTITY),
				String.valueOf(SHIPPING_PRICE),
				String.valueOf(TOTAL_PRICE)
		};
		String[] columnNames = new String[]{
				OrderItemFieldSetMapper.DISCOUNT_AMOUNT_COLUMN,
				OrderItemFieldSetMapper.DISCOUNT_PERC_COLUMN,
				OrderItemFieldSetMapper.HANDLING_PRICE_COLUMN,
				OrderItemFieldSetMapper.ITEM_ID_COLUMN,
				OrderItemFieldSetMapper.PRICE_COLUMN,
				OrderItemFieldSetMapper.QUANTITY_COLUMN,
				OrderItemFieldSetMapper.SHIPPING_PRICE_COLUMN,
				OrderItemFieldSetMapper.TOTAL_PRICE_COLUMN
		};
		return new DefaultFieldSet(tokens, columnNames);
	}

	protected FieldSetMapper<LineItem> fieldSetMapper() {
		return new OrderItemFieldSetMapper();
	}

}
