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

import org.springframework.batch.execution.tasklet.RestartableItemOrientedTasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.restart.GenericRestartData;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.restart.Restartable;
import org.springframework.batch.support.PropertiesConverter;

/**
 * @author Peter Zozom
 */
public class RestartableItemOrientedTaskletTests extends TestCase {

	private static class MockProvider implements ItemReader, Restartable {

		RestartData data = new RestartData() {

			public Properties getProperties() {
				return PropertiesConverter.stringToProperties("a=b");
			}
			
		};
		
		public Object read() {
			return null;
		}

		public RestartData getRestartData() {
			return data;
		}

		public void restoreFrom(RestartData data) {
			// restart data should be same as returned by getRestartData
			assertEquals(this.data.getProperties(), data.getProperties());
		}

		public Object getKey(Object item) {
			return null;
		}

		public boolean recover(Object data, Throwable cause) {
			return false;
		}

	}

	private static class MockProcessor implements ItemProcessor, Restartable {

		RestartData data = new RestartData() {
			public Properties getProperties() {
				return PropertiesConverter.stringToProperties("x=y");
			}
		};

		public void process(Object data) {
		}

		public RestartData getRestartData() {
			return data;
		}

		public void restoreFrom(RestartData data) {
			// restart data should be same as returned by getRestartData
			assertEquals(this.data.getProperties(), data.getProperties());
		}

	}

	private ItemReader itemProvider;

	private ItemProcessor itemProcessor;

	private RestartableItemOrientedTasklet module;

	public void testRestart() {

		// create data provider and data processor
		itemProvider = new MockProvider();
		itemProcessor = new MockProcessor();

		// create and set up module
		module = new RestartableItemOrientedTasklet();
		module.setItemProvider(itemProvider);
		module.setItemProcessor(itemProcessor);

		// get restart data
		RestartData data = module.getRestartData();
		assertNotNull(data);
		// restore from restart data (see asserts in mock classes)
		module.restoreFrom(data);
	}

	public void testRestartFromGenericData() {

		// create data provider and data processor
		itemProvider = new MockProvider();
		itemProcessor = new MockProcessor();

		// create and set up module
		module = new RestartableItemOrientedTasklet();
		module.setItemProvider(itemProvider);
		module.setItemProcessor(itemProcessor);

		// get restart data
		RestartData data = module.getRestartData();
		assertNotNull(data);
		data = new GenericRestartData(data.getProperties());
		// restore from restart data (see asserts in mock classes)
		module.restoreFrom(data);
	}
	
	public void testRestartFromNotRestartable() {

		// create and set up module
		module = new RestartableItemOrientedTasklet();
		module.setItemProvider(null);
		module.setItemProcessor(null);

		// get restart data
		RestartData data = module.getRestartData();
		assertNotNull(data);
		// restore from restart data (see asserts in mock classes)
		module.restoreFrom(data);
		//System.err.println(data.getProperties());
	}
	
}
