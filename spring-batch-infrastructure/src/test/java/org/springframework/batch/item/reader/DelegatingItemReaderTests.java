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

package org.springframework.batch.item.reader;

import java.util.Properties;

import junit.framework.TestCase;

import org.springframework.batch.io.Skippable;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.support.PropertiesConverter;

/**
 * Unit test for {@link DelegatingItemReader}
 * 
 * @author Robert Kasanicky
 */
public class DelegatingItemReaderTests extends TestCase {

	// object under test
	private DelegatingItemReader itemProvider = new DelegatingItemReader();

	private ItemReader source;
	
	private ExecutionContext executionContext;

	// create input template and inject it to data provider
	protected void setUp() throws Exception {
		executionContext = new ExecutionContext();
		source = new MockItemReader(this, executionContext);
		itemProvider.setItemReader(source);
	}

	public void testAfterPropertiesSet() throws Exception {
		// shouldn't throw an exception since the input source is set
		itemProvider.afterPropertiesSet();
	}

	public void testNullItemReader() {
		try {
			itemProvider.setItemReader(null);
			itemProvider.afterPropertiesSet();
			fail();
		}
		catch (Exception ex) {
			assertTrue(ex instanceof IllegalArgumentException);
		}
	}

	/**
	 * Uses input template to provide the domain object.
	 * @throws Exception
	 */
	public void testNext() throws Exception {
		Object result = itemProvider.read();
		assertSame("domain object is provided by the input template", this, result);
	}

	/**
	 * Gets restart data from the input template
	 */
	public void testGetStreamContext() {
		itemProvider.beforeSave();
		assertEquals("foo", executionContext.getString("value"));
	}

	public void testSkip() throws Exception {
		itemProvider.skip();
		assertEquals("after skip", itemProvider.read());
	}

	private static class MockItemReader extends AbstractItemReader implements ItemReader, ItemStream, Skippable {

		private Object value;
		private ExecutionContext executionContext;

		public Properties getStatistics() {
			return PropertiesConverter.stringToProperties("a=b");
		}

		public void beforeSave() {
			executionContext.putString("value", "foo");
		}

		public MockItemReader(Object value, ExecutionContext executionContext) {
			this.value = value;
			this.executionContext = executionContext;
		}

		public Object read() {
			return value;
		}

		public void close() {
		}

		public void open(ExecutionContext executionContext) {
		}

		public void skip() {
			value = "after skip";
		}

		public boolean isMarkSupported() {
			return false;
		}

		public void mark() {
		}

		public void reset() {
		}

	}

}
