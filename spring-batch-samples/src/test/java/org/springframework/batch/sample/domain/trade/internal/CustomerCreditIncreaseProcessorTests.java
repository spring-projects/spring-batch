/*
 * Copyright 2008 the original author or authors.
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
package org.springframework.batch.sample.domain.trade.internal;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Test;
import org.springframework.batch.sample.domain.trade.CustomerCredit;

/**
 * Tests for {@link CustomerCreditIncreaseProcessor}.
 * 
 * @author Robert Kasanicky
 */
public class CustomerCreditIncreaseProcessorTests {
	private CustomerCreditIncreaseProcessor tested = new CustomerCreditIncreaseProcessor();

	/*
	 * Increases customer's credit by fixed value
	 */
	@Test
	public void testProcess() throws Exception {
		final BigDecimal oldCredit = new BigDecimal("10.54");
		CustomerCredit customerCredit = new CustomerCredit();
		customerCredit.setCredit(oldCredit);
		
		assertEquals(oldCredit.add(CustomerCreditIncreaseProcessor.FIXED_AMOUNT),tested.process(customerCredit).getCredit());
	}
}
