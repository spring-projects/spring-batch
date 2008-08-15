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

import junit.framework.TestCase;

import org.springframework.batch.item.ClearFailedException;
import org.springframework.batch.item.FlushFailedException;
import org.springframework.batch.item.ItemWriter;

/**
 * Unit test class that was used as part of the Reference Documentation. I'm
 * only including it in the code to help keep the reference documentation up to
 * date as the code base shifts.
 * 
 * @author Lucas Ward
 * 
 */
public class CustomItemWriterTests extends TestCase {

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
	}
	
	public void testFlush() throws Exception{
		
		CustomItemWriter<String> itemWriter = new CustomItemWriter<String>();
		itemWriter.write("1");
		assertEquals(0, itemWriter.getOutput().size());
		itemWriter.flush();
		assertEquals(1, itemWriter.getOutput().size());
		itemWriter.write("2");
		itemWriter.write("3");
		itemWriter.clear();
		assertEquals(1, itemWriter.getOutput().size());
	}

	public class CustomItemWriter<T> implements ItemWriter<T>{
		
		List<T> output = new ArrayList<T>();
		List<T> buffer = new ArrayList<T>();

		public void write(T item) throws Exception {
			buffer.add(item);
		}
		
		public void clear() throws ClearFailedException {
			buffer.clear();
		}

		public void flush() throws FlushFailedException {
			for(T t:buffer){
				output.add(t);
			}
		}
		
		public List<T> getOutput() {
			return output;
		}
	}
}
