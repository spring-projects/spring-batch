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
package org.springframework.batch.statistics;

import java.util.Properties;

import org.springframework.batch.support.PropertiesConverter;

import junit.framework.TestCase;

/**
 * @author Dave Syer
 * 
 */
public class SimpleStatisticsServiceTests extends TestCase {

	private SimpleStatisticsService service = new SimpleStatisticsService();

	public void testRegistration() throws Exception {
		service.register("FOO", new StubStatisticsProvider());
		assertEquals("bar", service.getStatistics("FOO").getProperty("foo"));
	}

	public void testAggregation() throws Exception {
		service.register("FOO", new StubStatisticsProvider());
		service.register("FOO", new StubStatisticsProvider("spam=bucket"));
		assertEquals("bar", service.getStatistics("FOO").getProperty("foo"));
		assertEquals("bucket", service.getStatistics("FOO").getProperty("spam"));
	}

	public void testDoubleAggregationOrder() throws Exception {
		StubStatisticsProvider provider = new StubStatisticsProvider();
		service.register("FOO", provider);
		service.register("FOO", provider);
		assertEquals(1, service.getStatistics("FOO").size());
	}

	/**
	 * @author Dave Syer
	 * 
	 */
	private class StubStatisticsProvider implements StatisticsProvider {
		String values = "foo=bar";
		
		public StubStatisticsProvider(String values) {
			super();
			this.values = values;
		}

		public StubStatisticsProvider() {
			super();
		}

		public Properties getStatistics() {
			return PropertiesConverter.stringToProperties(values);
		}
	}

}
