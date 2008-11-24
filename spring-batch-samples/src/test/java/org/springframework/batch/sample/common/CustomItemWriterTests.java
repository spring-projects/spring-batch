/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.sample.common;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;

/**
 * Unit test class that was used as part of the Reference Documentation. I'm
 * only including it in the code to help keep the reference documentation up to
 * date as the code base shifts.
 * 
 * @author Lucas Ward
 * 
 */
public class CustomItemWriterTests {

	@Test
	public void testFlush() throws Exception {

		CustomItemWriter<String> itemWriter = new CustomItemWriter<String>();
		itemWriter.write(Collections.singletonList("1"));
		assertEquals(1, itemWriter.getOutput().size());
		itemWriter.write(Arrays.asList(new String[] {"2","3"}));
		assertEquals(3, itemWriter.getOutput().size());
	}

	public static class CustomItemWriter<T> implements ItemWriter<T> {

		List<T> output = TransactionAwareProxyFactory.createTransactionalList();

		public void write(List<? extends T> items) throws Exception {
			output.addAll(items);
		}

		public List<T> getOutput() {
			return output;
		}
	}
}
