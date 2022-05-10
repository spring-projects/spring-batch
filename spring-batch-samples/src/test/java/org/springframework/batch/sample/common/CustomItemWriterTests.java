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
package org.springframework.batch.sample.common;

import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test class that was used as part of the Reference Documentation. I'm only
 * including it in the code to help keep the reference documentation up to date as the
 * code base shifts.
 *
 * @author Lucas Ward
 * @author Mahmoud Ben Hassine
 * @author Glenn Renfro
 * 
 */
class CustomItemWriterTests {

	@Test
	void testFlush() throws Exception {
		CustomItemWriter<String> itemWriter = new CustomItemWriter<>();
		itemWriter.write(Chunk.of("1"));
		assertEquals(1, itemWriter.getOutput().size());
		itemWriter.write(Chunk.of("2", "3"));
		assertEquals(3, itemWriter.getOutput().size());
	}

	static class CustomItemWriter<T> implements ItemWriter<T> {

		private List<T> output = TransactionAwareProxyFactory.createTransactionalList();

		@Override
		public void write(Chunk<? extends T> chunk) throws Exception {
			output.addAll(chunk.getItems());
		}

		public List<T> getOutput() {
			return output;
		}

	}

}
