/*
 * Copyright 2008-2022 the original author or authors.
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
package org.springframework.batch.sample.domain.trade;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TradeTests {

	@Test
	void testEquality() {
		Trade trade1 = new Trade("isin", 1, new BigDecimal("1.1"), "customer1");
		Trade trade1Clone = new Trade("isin", 1, new BigDecimal("1.1"), "customer1");
		Trade trade2 = new Trade("isin", 1, new BigDecimal("2.3"), "customer2");

		assertEquals(trade1, trade1Clone);
		assertNotEquals(trade1, trade2);
	}

}
