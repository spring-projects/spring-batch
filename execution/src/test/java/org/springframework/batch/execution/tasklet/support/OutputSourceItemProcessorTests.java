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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.springframework.batch.io.OutputSource;
import org.springframework.batch.io.Skippable;
import org.springframework.batch.item.processor.OutputSourceItemProcessor;
import org.springframework.batch.restart.GenericRestartData;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.restart.Restartable;
import org.springframework.batch.statistics.StatisticsProvider;
import org.springframework.batch.support.PropertiesConverter;

/**
 * @author Dave Syer
 * 
 */
public class OutputSourceItemProcessorTests extends TestCase {

	private OutputSourceItemProcessor processor = new OutputSourceItemProcessor();

	private OutputSource source;

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		source = new MockOutputSource("test");
		processor.setOutputSource(source);
	}

	public void testProcess() throws Exception {
		processor.process("foo");
		assertEquals(1, list.size());
		assertEquals("test:foo", list.get(0));
	}
	
	/**
	 * Gets statistics from the input template
	 */
	public void testGetStatistics() {
		Properties props = processor.getStatistics();
		assertEquals("b", props.getProperty("a"));
	}

	/**
	 * Gets restart data from the input template
	 */
	public void testGetRestartData() {
		Properties props = processor.getRestartData().getProperties();
		assertEquals("foo", props.getProperty("value"));
	}

	/**
	 * Forward restart data to input template
	 * @throws Exception 
	 */
	public void testRestoreFrom() throws Exception {
		processor.restoreFrom(new GenericRestartData(PropertiesConverter.stringToProperties("value=bar")));
		processor.process("foo");
		assertEquals("bar:foo", list.get(0));
	}

	/**
	 * Forward restart data to input template
	 * @throws Exception 
	 */
	public void testGetRestartDataWithoutRestartable() throws Exception {
		processor.setOutputSource(null);
		try {
			processor.getRestartData();
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
	}

	/**
	 * Forward restart data to input template
	 * @throws Exception 
	 */
	public void testRestoreFromWithoutRestartable() throws Exception {
		processor.setOutputSource(null);
		try {
			processor.restoreFrom(new GenericRestartData(PropertiesConverter.stringToProperties("value=bar")));
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
	}

	/**
	 * Gets statistics from the input template
	 */
	public void testGetStatisticsWithoutStatisticsProvider() {
		processor.setOutputSource(null);
		Properties props = processor.getStatistics();
		assertEquals(null, props.getProperty("a"));
	}
	
	public void testSkip() {
		processor.skip();
		assertEquals(1, list.size());
		assertEquals("after skip", list.get(0));
	}

	private List list = new ArrayList(); 

	/**
	 * @author Dave Syer
	 * 
	 */
	public class MockOutputSource implements OutputSource, StatisticsProvider, Restartable, Skippable {

		private String value;

		public MockOutputSource(String string) {
			this.value = string;
		}

		public void write(Object output) {
			list.add(value+":"+output);
		}

		public void close() {
		}

		public void open() {
		}

		public Properties getStatistics() {
			return PropertiesConverter.stringToProperties("a=b");
		}

		public RestartData getRestartData() {
			return new GenericRestartData(PropertiesConverter.stringToProperties("value=foo"));
		}

		public void restoreFrom(RestartData data) {
			value = data.getProperties().getProperty("value");
		}

		public void skip() {
			list.add("after skip");	
		}

	}

}
