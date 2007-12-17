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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.springframework.batch.io.Skippable;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemRecoverer;
import org.springframework.batch.item.provider.AbstractItemReader;
import org.springframework.batch.repeat.context.RepeatContextSupport;
import org.springframework.batch.repeat.synch.RepeatSynchronizationManager;
import org.springframework.batch.retry.policy.SimpleRetryPolicy;
import org.springframework.batch.statistics.StatisticsProvider;
import org.springframework.batch.support.PropertiesConverter;

/**
 * @author Dave Syer
 * @author Peter Zozom
 */
public class ItemOrientedTaskletTests extends TestCase {

	private List list = new ArrayList();

	private List items = new ArrayList();

	private ItemReader itemProvider = new AbstractItemReader() {
		int count = 0;

		public Object read() throws Exception {
			if (count < items.size()) {
				Object data = items.get(count++);
				if (data instanceof Exception) {
					throw (Exception) data;
				}
				return data;
			}
			return null;
		}
	};

	private ItemProcessor itemProcessor = new ItemProcessor() {
		public void process(Object data) throws Exception {
			list.add(data);
		}
	};

	private ItemOrientedTasklet module;

	public void setUp() throws Exception {

		// create module
		module = new ItemOrientedTasklet();

		// set up module
		module.setItemProvider(itemProvider);
		module.setItemProcessor(itemProcessor);

		module.afterPropertiesSet();

		RepeatSynchronizationManager.register(new RepeatContextSupport(null));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		RepeatSynchronizationManager.clear();
	}

	// tests also read and process
	public void testExecute() throws Exception {

		// TEST1: data provider returns some object and data processor should
		// process it

		// set up mock objects
		items = Collections.singletonList("foo");

		// call execute
		assertTrue(module.execute().isContinuable());

		// verify method calls
		assertEquals(1, list.size());
		assertEquals("foo", list.get(0));
	}

	public void testExecuteWithNothingToRead() throws Exception {

		// TEST2: data provider returns null (nothing to read)

		// call read
		assertFalse(module.execute().isContinuable());

	}

	public void testExecuteWithExceptionOnRead() throws Exception {

		// TEST3: exception is thrown by data provider

		// set up mock objects
		items = Collections.singletonList(new RuntimeException("foo"));

		// call read
		try {
			module.execute();
			// TODO: should we expect Batch exception?
			fail("RuntimeException was expected");
		} catch (RuntimeException bce) {
			// expected
		}
	}

	public void testNotSkippable() throws Exception {
		try {
			module.skip();
		} catch (Exception e) {
			// Unexpected
			throw e;
		}
	}

	public void testSkippableReader() throws Exception {
		module.setItemProvider(new SkippableItemReader());
		module.setItemRecoverer(null);
		module.skip();
		assertEquals(1, list.size());
	}

	public void testSkippablReaderProcessor() throws Exception {
		module.setItemProvider(new SkippableItemReader());
		module.setItemProcessor(new SkippableItemProcessor());
		module.setItemRecoverer(null);
		module.skip();
		assertEquals(2, list.size());
	}

	public void testStatisticsProvider() throws Exception {
		module.setItemProvider(new SkippableItemReader());
		Properties stats = module.getStatistics();
		assertEquals(1, stats.size());
		assertEquals("bar", stats.getProperty("foo"));
	}

	public void testStatisticsProcessor() throws Exception {
		module.setItemProcessor(new SkippableItemProcessor());
		Properties stats = module.getStatistics();
		assertEquals(1, stats.size());
		assertEquals("bar", stats.getProperty("foo"));
	}

	public void testStatisticsProviderProcessor() throws Exception {
		module.setItemProvider(new SkippableItemReader());
		module.setItemProcessor(new SkippableItemProcessor());
		Properties stats = module.getStatistics();
		assertEquals(2, stats.size());
		assertEquals("bar", stats.getProperty("provider.foo"));
		assertEquals("bar", stats.getProperty("processor.foo"));
	}

	public void testStatisticsProviderProcessorMergeDuplicates()
			throws Exception {
		module.setItemProvider(new SkippableItemReader());
		module.setItemProcessor(new SkippableItemProcessor(
				"foo=bar\nspam=bucket"));
		Properties stats = module.getStatistics();
		assertEquals(3, stats.size());
		assertEquals("bar", stats.getProperty("provider.foo"));
		assertEquals("bar", stats.getProperty("processor.foo"));
		assertEquals("bucket", stats.getProperty("spam"));
	}

	public void testRecoverable() throws Exception {

		// set up and call execute
		items = Collections.singletonList("foo");

		module.setItemRecoverer(new ItemRecoverer() {
			public boolean recover(Object item, Throwable cause) {
				assertEquals("FOO", cause.getMessage());
				list.add(item);
				return true;
			}
		});

		module.setItemProvider(new AbstractItemReader() {
			public Object read() throws Exception {
				return "bar";
			}
		});

		module.setItemProcessor(new ItemProcessor() {
			public void process(Object data) throws Exception {
				throw new RuntimeException("FOO");
			}
		});

		module.afterPropertiesSet();

		try {
			module.execute();
			fail("Expected RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("FOO", e.getMessage());
		}

		// After a processing exception the recovery is done automatically.

		// verify method calls
		assertEquals(1, list.size());
		assertEquals("The item was not passed in to recover method", "bar",
				list.get(0));
	}

	public void testRetryPolicy() throws Exception {
		module.setRetryPolicy(new SimpleRetryPolicy(1));
		module.setItemRecoverer(new ItemRecoverer() {
			public boolean recover(Object item, Throwable cause) {
				assertEquals("FOO", cause.getMessage());
				list.add(item+"_recovered");
				return true;
			}
		});
		module.setItemProvider(new AbstractItemReader() {
			public Object read() throws Exception {
				return "foo";
			}
		});
		module.setItemProcessor(new ItemProcessor() {
			public void process(Object data) throws Exception {
				throw new RuntimeException("FOO");
			}
		});

		// finish initialisation
		module.afterPropertiesSet();

		try {
			module.execute();
			fail("Expected RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("FOO", e.getMessage());
		}

		// No exception thrown now because we are going to recover...
		module.execute();

		// No need for client has to call recover directly

		// verify method calls
		assertEquals(1, list.size());
		assertEquals("The item was not passed in to recover method",
				"foo_recovered", list.get(0));
	}

	public void testInitialisationWithNullProvider() throws Exception {
		module.setItemProvider(null);
		try {
			module.afterPropertiesSet();
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().toLowerCase().indexOf("provider") >= 0);
		}
	}

	public void testInitialisationWithNullProcessor() throws Exception {
		module.setItemProcessor(null);
		try {
			module.afterPropertiesSet();
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().toLowerCase().indexOf("processor") >= 0);
		}
	}

	private class SkippableItemReader implements ItemReader,
			Skippable, StatisticsProvider {
		public Object read() throws Exception {
			return itemProvider.read();
		}
		public Object getKey(Object item) {
			return item;
		}
		public void skip() {
			list.add("provider");
		}
		public Properties getStatistics() {
			return PropertiesConverter.stringToProperties("foo=bar");
		}
	}

	private class SkippableItemProcessor implements ItemProcessor, Skippable,
			StatisticsProvider {
		String props = "foo=bar";

		public SkippableItemProcessor() {
			super();
		}

		public SkippableItemProcessor(String props) {
			this();
			this.props = props;
		}

		public void process(Object data) throws Exception {
			// no-op
		}

		public void skip() {
			list.add("processor");
		}

		public Properties getStatistics() {
			return PropertiesConverter.stringToProperties(props);
		}
	}
}
