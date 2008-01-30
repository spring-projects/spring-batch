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

package org.springframework.batch.execution.tasklet;

import java.util.Properties;

import junit.framework.TestCase;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.StreamContext;
import org.springframework.batch.item.stream.GenericStreamContext;
import org.springframework.batch.support.PropertiesConverter;

/**
 * @author Peter Zozom
 */
public class RestartableItemOrientedTaskletTests extends TestCase {

	private static class MockProvider implements ItemReader, ItemStream {

		StreamContext data = new StreamContext() {

			public Properties getProperties() {
				return PropertiesConverter.stringToProperties("a=b");
			}
			
		};
		
		public Object read() {
			return null;
		}

		public StreamContext getRestartData() {
			return data;
		}

		public void restoreFrom(StreamContext data) {
			// restart data should be same as returned by getRestartData
			assertEquals(this.data.getProperties(), data.getProperties());
		}

		public boolean recover(Object data, Throwable cause) {
			return false;
		}

		public void close() throws Exception {			
		}

	}

	private static class MockWriter implements ItemWriter, ItemStream {

		StreamContext data = new StreamContext() {
			public Properties getProperties() {
				return PropertiesConverter.stringToProperties("x=y");
			}
		};

		public void write(Object data) {
		}

		public StreamContext getRestartData() {
			return data;
		}

		public void restoreFrom(StreamContext data) {
			// restart data should be same as returned by getRestartData
			assertEquals(this.data.getProperties(), data.getProperties());
		}

		public void close() throws Exception {
		}

	}

	private ItemReader itemProvider;

	private ItemWriter itemWriter;

	private RestartableItemOrientedTasklet module;

	public void testRestart() {

		// create data provider and data processor
		itemProvider = new MockProvider();
		itemWriter = new MockWriter();

		// create and set up module
		module = new RestartableItemOrientedTasklet();
		module.setItemReader(itemProvider);
		module.setItemWriter(itemWriter);

		// get restart data
		StreamContext data = module.getRestartData();
		assertNotNull(data);
		// restore from restart data (see asserts in mock classes)
		module.restoreFrom(data);
	}

	public void testRestartFromGenericData() {

		// create data provider and data processor
		itemProvider = new MockProvider();
		itemWriter = new MockWriter();

		// create and set up module
		module = new RestartableItemOrientedTasklet();
		module.setItemReader(itemProvider);
		module.setItemWriter(itemWriter);

		// get restart data
		StreamContext data = module.getRestartData();
		assertNotNull(data);
		data = new GenericStreamContext(data.getProperties());
		// restore from restart data (see asserts in mock classes)
		module.restoreFrom(data);
	}
	
	public void testRestartFromNotRestartable() {

		// create and set up module
		module = new RestartableItemOrientedTasklet();
		module.setItemReader(null);
		module.setItemWriter(null);

		// get restart data
		StreamContext data = module.getRestartData();
		assertNotNull(data);
		// restore from restart data (see asserts in mock classes)
		module.restoreFrom(data);
		//System.err.println(data.getProperties());
	}
	
}
