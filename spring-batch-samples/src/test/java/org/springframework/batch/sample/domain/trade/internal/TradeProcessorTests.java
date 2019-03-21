/*
 * Copyright 2008-2013 the original author or authors.
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

import static org.mockito.Mockito.mock;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.sample.domain.trade.Trade;
import org.springframework.batch.sample.domain.trade.TradeDao;

public class TradeProcessorTests {
	private TradeDao writer;
	private TradeWriter processor;
	
	@Before
	public void setUp() {
		writer = mock(TradeDao.class);
		
		processor = new TradeWriter();
		processor.setDao(writer);
	}
		
	@Test
	public void testProcess() {
		Trade trade = new Trade();

		writer.writeTrade(trade);
		
		processor.write(Collections.singletonList(trade));
	}
}
