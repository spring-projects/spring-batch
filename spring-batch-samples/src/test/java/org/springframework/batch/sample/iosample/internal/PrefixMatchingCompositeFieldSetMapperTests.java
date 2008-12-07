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

package org.springframework.batch.sample.iosample.internal;

import static junit.framework.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.batch.sample.domain.trade.Trade;
import org.springframework.batch.sample.domain.trade.internal.CustomerCreditFieldSetMapper;
import org.springframework.batch.sample.domain.trade.internal.TradeFieldSetMapper;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public class PrefixMatchingCompositeFieldSetMapperTests {

	@Test
	public void testMapFieldSet() {
		Map<String, FieldSetMapper> mappers = new HashMap<String, FieldSetMapper>();
		mappers.put("TRAD", new TradeFieldSetMapper());
		mappers.put("CUST", new CustomerCreditFieldSetMapper());

		PrefixMatchingCompositeFieldSetMapper mapper = new PrefixMatchingCompositeFieldSetMapper();
		mapper.setMappers(mappers);

		String[] tradeNames = new String[] { "isin", "quantity", "price", "customer", "prefix" };
		String[] tradeValues = new String[] { "ISIN001", "500", "4.50", "Customer1", "TRAD" };
		FieldSet tradeFS = new DefaultFieldSet(tradeValues, tradeNames);

		String[] customerNames = new String[] { "id", "name", "credit", "prefix" };
		String[] customerValues = new String[] { "256", "customer1", "3200.00", "CUST" };
		FieldSet customerFS = new DefaultFieldSet(customerValues, customerNames);

		Trade trade = new Trade("ISIN001", 500, new BigDecimal("4.50"), "Customer1");
		assertEquals(trade, mapper.mapFieldSet(tradeFS));

		CustomerCredit customer = new CustomerCredit(256, "customer1", new BigDecimal("3200.00"));
		assertEquals(customer, mapper.mapFieldSet(customerFS));
	}
}
