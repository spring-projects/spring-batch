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
package org.springframework.batch.item.writer;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.springframework.batch.io.Skippable;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.StreamContext;
import org.springframework.batch.support.PropertiesConverter;

/**
 * @author Dave Syer
 * 
 */
public class ItemWriterItemProcessorTests extends TestCase {

	private DelegatingItemWriter processor = new DelegatingItemWriter();

	private ItemWriter writer;

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		writer = new MockOutputSource("test");
		processor.setDelegate(writer);
		processor.afterPropertiesSet();
	}

	public void testProcess() throws Exception {
		processor.write("foo");
		assertEquals(1, list.size());
		assertEquals("test:foo", list.get(0));
	}

	/**
	 * Gets restart data from the input template
	 */
	public void testGetStreamContext() {
		Properties props = processor.getStreamContext().getProperties();
		assertEquals("foo", props.getProperty("value"));
	}

	/**
	 * Forward restart data to input template
	 * @throws Exception
	 */
	public void testRestoreFrom() throws Exception {
		processor.restoreFrom(new StreamContext(PropertiesConverter.stringToProperties("value=bar")));
		processor.write("foo");
		assertEquals("bar:foo", list.get(0));
	}

	/**
	 * Forward restart data to input template
	 * @throws Exception
	 */
	public void testGetStreamContextWithoutItemStream() throws Exception {
		processor.setDelegate(null);
		try {
			processor.getStreamContext();
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
		processor.setDelegate(null);
		try {
			processor.restoreFrom(new StreamContext(PropertiesConverter.stringToProperties("value=bar")));
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
	}

	public void testSkip() {
		processor.skip();
		assertEquals(1, list.size());
		assertEquals("after skip", list.get(0));
	}

	/**
	 * ItemWriter property must be set.
	 */
	public void testAfterPropertiesSet() throws Exception {
		processor.setDelegate(null);
		try {
			processor.afterPropertiesSet();
			fail();
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	private List list = new ArrayList();

	/**
	 * @author Dave Syer
	 * 
	 */
	public class MockOutputSource extends AbstractItemStreamItemWriter implements Skippable {

		private String value;

		public MockOutputSource(String string) {
			this.value = string;
		}

		public void write(Object output) {
			list.add(value + ":" + output);
		}

		public void close() {
		}

		public void open() {
		}

		public StreamContext getStreamContext() {
			return new StreamContext(PropertiesConverter.stringToProperties("value=foo"));
		}

		public void restoreFrom(StreamContext data) {
			value = data.getProperties().getProperty("value");
		}

		public void skip() {
			list.add("after skip");
		}

	}

}
