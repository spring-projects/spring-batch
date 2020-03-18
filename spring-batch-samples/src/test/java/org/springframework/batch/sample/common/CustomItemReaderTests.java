/*
 * Copyright 2006-2019 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.lang.Nullable;

/**
 * Unit test class that was used as part of the Reference Documentation.  I'm only including it in the
 * code to help keep the reference documentation up to date as the code base shifts.
 * 
 * @author Lucas Ward
 *
 */
public class CustomItemReaderTests {
	private ItemReader<String> itemReader;
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Before
	public void setUp() throws Exception {
		List<String> items = new ArrayList<>();
		items.add("1");
		items.add("2");
		items.add("3");
		
		itemReader = new CustomItemReader<>(items);
	}
	
	@Test
	public void testRead() throws Exception{
		assertEquals("1", itemReader.read());
		assertEquals("2", itemReader.read());
		assertEquals("3", itemReader.read());
		assertNull(itemReader.read());
	}
	
	@Test
	public void testRestart() throws Exception{
		ExecutionContext executionContext = new ExecutionContext();
		((ItemStream)itemReader).open(executionContext);
		assertEquals("1", itemReader.read());
		((ItemStream)itemReader).update(executionContext);
		List<String> items = new ArrayList<>();
		items.add("1");
		items.add("2");
		items.add("3");
		itemReader = new CustomItemReader<>(items);
		
		((ItemStream)itemReader).open(executionContext);
		assertEquals("2", itemReader.read());
	}

	public static class CustomItemReader<T> implements ItemReader<T>, ItemStream {
		private static final String CURRENT_INDEX = "current.index";

		private List<T> items;
		private int currentIndex = 0;

		public CustomItemReader(List<T> items) {
			this.items = items;
		}

		@Nullable
		@Override
		public T read() throws Exception {
			if (currentIndex < items.size()) {
				return items.get(currentIndex++);
			}
			return null;
		}

		@Override
		public void open(ExecutionContext executionContext) throws ItemStreamException {
			if(executionContext.containsKey(CURRENT_INDEX)){
				currentIndex = executionContext.getInt(CURRENT_INDEX);
			}
			else{
				currentIndex = 0;
			}
		}

		@Override
		public void close() throws ItemStreamException {}

		@Override
		public void update(ExecutionContext executionContext) throws ItemStreamException {
			executionContext.putInt(CURRENT_INDEX, currentIndex);
		}
	}
}
