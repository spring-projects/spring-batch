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
package org.springframework.batch.sample.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;
import org.springframework.batch.sample.domain.trade.Trade;
import org.springframework.batch.sample.domain.trade.internal.ItemTrackingTradeItemWriter;

/**
 * @author Dave Syer
 * 
 */
public class ItemTrackingItemWriterTests {

	private ItemTrackingTradeItemWriter writer = new ItemTrackingTradeItemWriter();

	/**
	 * Test method for
	 * {@link org.springframework.batch.sample.domain.trade.internal.ItemTrackingTradeItemWriter#write(java.util.List)}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testWrite() throws Exception {
		assertEquals(0, writer.getItems().size());
		Trade a = new Trade("a", 0, null, null);
		Trade b = new Trade("b", 0, null, null);
		Trade c = new Trade("c", 0, null, null);
		writer.write(Arrays.asList(a, b, c));
		assertEquals(3, writer.getItems().size());
	}

	@Test
	public void testWriteFailure() throws Exception {
		writer.setWriteFailureISIN("c");
		try {
			Trade a = new Trade("a", 0, null, null);
			Trade b = new Trade("b", 0, null, null);
			Trade c = new Trade("c", 0, null, null);
			writer.write(Arrays.asList(a, b, c));
			fail("Expected Write Failure Exception");
		}
		catch (RuntimeException e) {
			// expected
		}
		// the failed item is removed
		assertEquals(0, writer.getItems().size());

		Trade e = new Trade("e", 0, null, null);
		Trade f = new Trade("f", 0, null, null);
		Trade g = new Trade("g", 0, null, null);
		writer.write(Arrays.asList(e, f, g));
		assertEquals(3, writer.getItems().size());
	}
}
