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
package org.springframework.batch.sample.domain.trade.internal;

import java.math.BigDecimal;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.sample.domain.trade.Trade;
import org.springframework.batch.sample.support.AbstractFieldSetMapperTests;

public class TradeFieldSetMapperTests extends AbstractFieldSetMapperTests {
	private static final String CUSTOMER = "Mike Tomcat";
	private static final BigDecimal PRICE = new BigDecimal(1.3);
	private static final long QUANTITY = 7;
	private static final String ISIN = "fj893gnsalX";

	@Override
	protected Object expectedDomainObject() {
		Trade trade = new Trade();
		trade.setIsin(ISIN);
		trade.setQuantity(QUANTITY);
		trade.setPrice(PRICE);
		trade.setCustomer(CUSTOMER);
		return trade;
	}

	@Override
	protected FieldSet fieldSet() {
		String[] tokens = new String[4];
		tokens[TradeFieldSetMapper.ISIN_COLUMN] = ISIN;
		tokens[TradeFieldSetMapper.QUANTITY_COLUMN] = String.valueOf(QUANTITY);
		tokens[TradeFieldSetMapper.PRICE_COLUMN] = String.valueOf(PRICE);
		tokens[TradeFieldSetMapper.CUSTOMER_COLUMN] = CUSTOMER;

		return new DefaultFieldSet(tokens);
	}

	@Override
	protected FieldSetMapper<Trade> fieldSetMapper() {
		return new TradeFieldSetMapper();
	}
}
