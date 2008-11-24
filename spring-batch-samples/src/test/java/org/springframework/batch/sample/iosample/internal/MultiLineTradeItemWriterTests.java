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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.sample.domain.trade.Trade;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public class MultiLineTradeItemWriterTests {

	private MultiLineTradeItemWriter writer;

	public MultiLineTradeItemWriterTests() {
		FlatFileItemWriter<String> delegate = new FlatFileItemWriter<String>() {
			List<String> allItems = new ArrayList<String>();

			public void write(List items) throws Exception {
				this.allItems.addAll(items);
			}

			public List<String> getAllItems() {
				return this.allItems;
			}
		};

		this.writer = new MultiLineTradeItemWriter();
		this.writer.setDelegate(delegate);
	}

	@Test
	public void testWrite() throws Exception {
		Trade t1 = new Trade("ISIN001", 400, new BigDecimal("5.75"), "Customer1");
		Trade t2 = new Trade("ISIN002", 200, new BigDecimal("6.25"), "Customer2");
		this.writer.write(Arrays.asList(t1, t2));
	}
}
