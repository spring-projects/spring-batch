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
import java.util.Arrays;
import java.util.Collections;
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

	public void testFlush() throws Exception {

		CustomItemWriter<String> itemWriter = new CustomItemWriter<String>();
		itemWriter.write(Collections.singletonList("1"));
		assertEquals(0, itemWriter.getOutput().size());
		itemWriter.flush();
		assertEquals(1, itemWriter.getOutput().size());
		itemWriter.write(Arrays.asList(new String[] {"2","3"}));
		itemWriter.clear();
		assertEquals(1, itemWriter.getOutput().size());
	}

	public class CustomItemWriter<T> implements ItemWriter<T> {

		List<T> output = new ArrayList<T>();

		List<T> buffer = new ArrayList<T>();

		public void write(List<? extends T> items) throws Exception {
			buffer.addAll(items);
		}

		public void clear() throws ClearFailedException {
			buffer.clear();
		}

		public void flush() throws FlushFailedException {
			output.addAll(buffer);
		}

		public List<T> getOutput() {
			return output;
		}
	}
}
