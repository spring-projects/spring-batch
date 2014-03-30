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

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.Test;
import org.springframework.batch.sample.domain.trade.CustomerDebit;
import org.springframework.batch.sample.domain.trade.CustomerDebitDao;
import org.springframework.batch.sample.domain.trade.Trade;

public class CustomerUpdateProcessorTests {
	@Test
	public void testProcess() {
		Trade trade = new Trade();
		trade.setCustomer("testCustomerName");
		trade.setPrice(new BigDecimal("123.0"));
		
		CustomerDebitDao dao = new CustomerDebitDao() {
			@Override
			public void write(CustomerDebit customerDebit) {
				assertEquals("testCustomerName", customerDebit.getName());
				assertEquals(new BigDecimal("123.0"), customerDebit.getDebit());
			}			
		};
		
		CustomerUpdateWriter processor = new CustomerUpdateWriter();
		processor.setDao(dao);
		
		processor.write(Collections.singletonList(trade));
	}
}
