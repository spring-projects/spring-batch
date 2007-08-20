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

package org.springframework.batch.execution.tasklet.support;

import java.util.Properties;

import junit.framework.TestCase;

import org.springframework.batch.execution.tasklet.support.InputSourceItemProvider;
import org.springframework.batch.io.InputSource;
import org.springframework.batch.io.Skippable;
import org.springframework.batch.restart.GenericRestartData;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.restart.Restartable;
import org.springframework.batch.statistics.StatisticsProvider;
import org.springframework.batch.support.PropertiesConverter;

/**
 * Unit test for {@link InputSourceItemProvider}
 * 
 * @author Robert Kasanicky
 */
public class InputSourceItemProviderTests extends TestCase {

	// object under test
	private InputSourceItemProvider itemProvider = new InputSourceItemProvider();

	private InputSource source;

	// create input template and inject it to data provider
	protected void setUp() throws Exception {
		source = new MockInputSource(this);
		itemProvider.setInputSource(source);
	}

	/**
	 * Uses input template to provide the domain object.
	 */
	public void testNext() {
		Object result = itemProvider.next();
		assertSame("domain object is provided by the input template", this, result);
	}

	/**
	 * Gets statistics from the input template
	 */
	public void testGetStatistics() {
		Properties props = itemProvider.getStatistics();
		assertEquals("b", props.getProperty("a"));
	}

	/**
	 * Gets restart data from the input template
	 */
	public void testGetRestartData() {
		Properties props = itemProvider.getRestartData().getProperties();
		assertEquals("foo", props.getProperty("value"));
	}

	/**
	 * Forwared restart data to input template
	 */
	public void testRestoreFrom() {
		itemProvider.restoreFrom(new GenericRestartData(PropertiesConverter.stringToProperties("value=bar")));
		assertEquals("bar", itemProvider.next());
	}
	
	public void testSkip() {
		itemProvider.skip();
		assertEquals("after skip", itemProvider.next());
	}

	private class MockInputSource implements InputSource, StatisticsProvider, Restartable, Skippable {

		private Object value;
		
		public Properties getStatistics() {
			return PropertiesConverter.stringToProperties("a=b");
		}

		public RestartData getRestartData() {
			return new GenericRestartData(PropertiesConverter.stringToProperties("value=foo"));
		}

		public void restoreFrom(RestartData data) {
			value = data.getProperties().getProperty("value");
		}

		public MockInputSource(Object value) {
			this.value = value;
		}

		public Object read() {
			return value;
		}

		public void close() {
		}

		public void open() {
		}

		public void skip() {
			value = "after skip";
		}

	}

}
