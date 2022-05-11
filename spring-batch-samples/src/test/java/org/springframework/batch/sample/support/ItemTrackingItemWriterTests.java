/*
 * Copyright 2006-2022 the original author or authors.
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
package org.springframework.batch.sample.support;

import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.sample.domain.trade.Trade;
import org.springframework.batch.sample.domain.trade.internal.ItemTrackingTradeItemWriter;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @author Glenn Renfro
 * 
 */
class ItemTrackingItemWriterTests {

	private final ItemTrackingTradeItemWriter writer = new ItemTrackingTradeItemWriter();

	@Test
	void testWrite() throws Exception {
		assertEquals(0, writer.getItems().size());
		Trade a = new Trade("a", 0, null, null);
		Trade b = new Trade("b", 0, null, null);
		Trade c = new Trade("c", 0, null, null);
		writer.write(Chunk.of(a, b, c));
		assertEquals(3, writer.getItems().size());
	}

	@Test
	void testWriteFailure() throws Exception {
		writer.setWriteFailureISIN("c");
		Trade a = new Trade("a", 0, null, null);
		Trade b = new Trade("b", 0, null, null);
		Trade c = new Trade("c", 0, null, null);
		assertThrows(IOException.class, () -> writer.write(Chunk.of(a, b, c)));
		assertEquals(0, writer.getItems().size());

		Trade e = new Trade("e", 0, null, null);
		Trade f = new Trade("f", 0, null, null);
		Trade g = new Trade("g", 0, null, null);
		writer.write(Chunk.of(e, f, g));
		assertEquals(3, writer.getItems().size());
	}

}
