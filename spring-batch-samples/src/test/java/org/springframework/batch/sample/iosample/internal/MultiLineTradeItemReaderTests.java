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
import static junit.framework.Assert.fail;

import java.math.BigDecimal;

import org.junit.Test;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultFieldSet;
import org.springframework.batch.item.file.mapping.FieldSet;
import org.springframework.batch.sample.domain.trade.Trade;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public class MultiLineTradeItemReaderTests {

	private MultiLineTradeItemReader reader = new MultiLineTradeItemReader();

	@Test
	public void testRead_complete() throws Exception {
		String isin = "ISIN001";
		String customer = "customer1";
		long quantity = (long) 300;
		BigDecimal price = new BigDecimal("4.50");

		final FieldSet[] data = { new DefaultFieldSet(new String[] { "BEGIN" }),
				new DefaultFieldSet(new String[] { "INFO", isin, customer }),
				new DefaultFieldSet(new String[] { "AMNT", "" + quantity, price.toString() }),
				new DefaultFieldSet(new String[] { "END" }) };

		Trade t = this.readData(data);
		assertEquals(isin, t.getIsin());
		assertEquals(customer, t.getCustomer());
		assertEquals(quantity, t.getQuantity());
		assertEquals(price, t.getPrice());
	}

	@Test
	public void testRead_noBegin() throws Exception {
		String isin = "ISIN001";
		String customer = "customer1";
		long quantity = (long) 300;
		BigDecimal price = new BigDecimal("4.50");

		final FieldSet[] data = { new DefaultFieldSet(new String[] { "INFO", isin, customer }),
				new DefaultFieldSet(new String[] { "AMNT", "" + quantity, price.toString() }),
				new DefaultFieldSet(new String[] { "END" }) };

		try {
			this.readData(data);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("No 'BEGIN' was found.", e.getMessage());
		}
	}

	@Test
	public void testRead_noEnd() throws Exception {
		String isin = "ISIN001";
		String customer = "customer1";
		long quantity = (long) 300;
		BigDecimal price = new BigDecimal("4.50");

		final FieldSet[] data = { new DefaultFieldSet(new String[] { "BEGIN" }),
				new DefaultFieldSet(new String[] { "INFO", isin, customer }),
				new DefaultFieldSet(new String[] { "AMNT", "" + quantity, price.toString() }) };

		try {
			this.readData(data);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("No 'END' was found.", e.getMessage());
		}
	}

	private Trade readData(final FieldSet[] data) throws Exception {
		this.reader.setDelegate(new FlatFileItemReader<FieldSet>() {
			private int i = 0;

			public FieldSet read() {
				if (i < data.length) {
					return data[i++];
				}
				return null;
			}
		});
		return this.reader.read();
	}
}
