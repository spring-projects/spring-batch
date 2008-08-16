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

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.MarkFailedException;
import org.springframework.batch.item.NoWorkFoundException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.ResetFailedException;
import org.springframework.batch.item.UnexpectedInputException;

import junit.framework.TestCase;

/**
 * Unit test class that was used as part of the Reference Documentation.  I'm only including it in the
 * code to help keep the reference documentation up to date as the code base shifts.
 * 
 * @author Lucas Ward
 *
 */
public class CustomItemReaderTests extends TestCase {

	ItemReader<String> itemReader;
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		
		List<String> items = new ArrayList<String>();
		items.add("1");
		items.add("2");
		items.add("3");
		
		itemReader = new CustomItemReader<String>(items);
	}
	
	public void testRead() throws Exception{
	
		assertEquals("1", itemReader.read());
		assertEquals("2", itemReader.read());
		assertEquals("3", itemReader.read());
		assertNull(itemReader.read());
	}
	
	public void testRollback() throws Exception{
		
		itemReader.mark();
		assertEquals("1", itemReader.read());
		assertEquals("2", itemReader.read());
		itemReader.reset();
		assertEquals("1", itemReader.read());
	}
	
	public void testRestart() throws Exception{
		
		ExecutionContext executionContext = new ExecutionContext();
		((ItemStream)itemReader).open(executionContext);
		assertEquals("1", itemReader.read());
		((ItemStream)itemReader).update(executionContext);
		List<String> items = new ArrayList<String>();
		items.add("1");
		items.add("2");
		items.add("3");
		itemReader = new CustomItemReader<String>(items);
		
		((ItemStream)itemReader).open(executionContext);
		assertEquals("2", itemReader.read());
	}

	public class CustomItemReader<T> implements ItemReader<T>, ItemStream{

		List<T> items;
		int currentIndex = 0;
		int lastMarkedIndex = 0;
		private static final String CURRENT_INDEX = "current.index";
		
		public CustomItemReader(List<T> items) {
			this.items = items;
		}

		public T read() throws Exception, UnexpectedInputException,
				NoWorkFoundException, ParseException {
			
			if (currentIndex < items.size()) {
				return items.get(currentIndex++);
			}
			return null;
		}
		
		public void mark() throws MarkFailedException {
			lastMarkedIndex = currentIndex;
		};

		public void reset() throws ResetFailedException {
			currentIndex = lastMarkedIndex;
		}
		
		public void open(ExecutionContext executionContext) throws ItemStreamException {
			if(executionContext.containsKey(CURRENT_INDEX)){
				currentIndex = new Long(executionContext.getLong(CURRENT_INDEX)).intValue();
			}
			else{
				currentIndex = 0;
				lastMarkedIndex = 0;
			}
		}

		public void close(ExecutionContext executionContext) throws ItemStreamException {}

		public void update(ExecutionContext executionContext) throws ItemStreamException {
			executionContext.putLong(CURRENT_INDEX, new Long(currentIndex).longValue());
		};
		
	}
}
