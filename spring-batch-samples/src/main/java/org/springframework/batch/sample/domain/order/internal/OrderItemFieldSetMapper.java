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

package org.springframework.batch.sample.domain.order.internal;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.sample.domain.order.LineItem;


public class OrderItemFieldSetMapper implements FieldSetMapper<LineItem> {
	
	public static final String TOTAL_PRICE_COLUMN = "TOTAL_PRICE";
	public static final String QUANTITY_COLUMN = "QUANTITY";
	public static final String HANDLING_PRICE_COLUMN = "HANDLING_PRICE";
	public static final String SHIPPING_PRICE_COLUMN = "SHIPPING_PRICE";
	public static final String DISCOUNT_AMOUNT_COLUMN = "DISCOUNT_AMOUNT";
	public static final String DISCOUNT_PERC_COLUMN = "DISCOUNT_PERC";
	public static final String PRICE_COLUMN = "PRICE";
	public static final String ITEM_ID_COLUMN = "ITEM_ID";
	
	
    public LineItem mapFieldSet(FieldSet fieldSet) {
        LineItem item = new LineItem();

        item.setItemId(fieldSet.readLong(ITEM_ID_COLUMN));
        item.setPrice(fieldSet.readBigDecimal(PRICE_COLUMN));
        item.setDiscountPerc(fieldSet.readBigDecimal(DISCOUNT_PERC_COLUMN));
        item.setDiscountAmount(fieldSet.readBigDecimal(DISCOUNT_AMOUNT_COLUMN));
        item.setShippingPrice(fieldSet.readBigDecimal(SHIPPING_PRICE_COLUMN));
        item.setHandlingPrice(fieldSet.readBigDecimal(HANDLING_PRICE_COLUMN));
        item.setQuantity(fieldSet.readInt(QUANTITY_COLUMN));
        item.setTotalPrice(fieldSet.readBigDecimal(TOTAL_PRICE_COLUMN));

        return item;
    }
}
