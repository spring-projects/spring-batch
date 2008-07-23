package org.springframework.batch.item.database;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.sample.Foo;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

public class DrivingQueryItemReaderTests extends TestCase {

	DrivingQueryItemReader<Foo> itemReader;

	static {
		TransactionSynchronizationManager.initSynchronization();
	}

	protected void setUp() throws Exception {
		super.setUp();

		itemReader = createItemReader();
	}

	private DrivingQueryItemReader<Foo> createItemReader() throws Exception {

		DrivingQueryItemReader<Foo> inputSource = new DrivingQueryItemReader<Foo>();
		inputSource.setKeyCollector(new MockKeyGenerator());
		inputSource.setSaveState(true);

		return inputSource;
	}

	/**
	 * Regular scenario - read all rows and eventually return null.
	 */
	public void testNormalProcessing() throws Exception {
		getAsInitializingBean(itemReader).afterPropertiesSet();
		getAsItemStream(itemReader).open(new ExecutionContext());

		Foo foo1 = (Foo) itemReader.read();
		assertEquals(1, foo1.getValue());

		Foo foo2 = (Foo) itemReader.read();
		assertEquals(2, foo2.getValue());

		Foo foo3 = (Foo) itemReader.read();
		assertEquals(3, foo3.getValue());

		Foo foo4 = (Foo) itemReader.read();
		assertEquals(4, foo4.getValue());

		Foo foo5 = (Foo) itemReader.read();
		assertEquals(5, foo5.getValue());

		assertNull(itemReader.read());
	}

	/**
	 * Restart scenario.
	 * 
	 * @throws Exception
	 */
	public void testRestart() throws Exception {

		ExecutionContext executionContext = new ExecutionContext();

		getAsItemStream(itemReader).open(executionContext);

		Foo foo1 = (Foo) itemReader.read();
		assertEquals(1, foo1.getValue());

		Foo foo2 = (Foo) itemReader.read();
		assertEquals(2, foo2.getValue());

		getAsItemStream(itemReader).update(executionContext);

		// create new input source
		itemReader = createItemReader();

		getAsItemStream(itemReader).open(executionContext);

		Foo fooAfterRestart = (Foo) itemReader.read();
		assertEquals(3, fooAfterRestart.getValue());
	}

	/**
	 * Reading from an input source and then trying to restore causes an error.
	 */
	public void testInvalidRestore() throws Exception {

		ExecutionContext executionContext = new ExecutionContext();

		getAsItemStream(itemReader).open(executionContext);

		Foo foo1 = (Foo) itemReader.read();
		assertEquals(1, foo1.getValue());

		Foo foo2 = (Foo) itemReader.read();
		assertEquals(2, foo2.getValue());

		getAsItemStream(itemReader).update(executionContext);

		// create new input source
		itemReader = createItemReader();
		getAsItemStream(itemReader).open(new ExecutionContext());

		Foo foo = (Foo) itemReader.read();
		assertEquals(1, foo.getValue());

		try {
			getAsItemStream(itemReader).open(executionContext);
			fail();
		} catch (IllegalStateException ex) {
			// expected
		}
	}

	/**
	 * Empty restart data should be handled gracefully.
	 * 
	 * @throws Exception
	 */
	public void testRestoreFromEmptyData() throws Exception {
		ExecutionContext streamContext = new ExecutionContext(new ExecutionContext());

		getAsItemStream(itemReader).open(streamContext);

		Foo foo = (Foo) itemReader.read();
		assertEquals(1, foo.getValue());
	}

	/**
	 * Rollback scenario.
	 * 
	 * @throws Exception
	 */
	public void testRollback() throws Exception {
		getAsItemStream(itemReader).open(new ExecutionContext());
		Foo foo1 = (Foo) itemReader.read();

		commit();

		Foo foo2 = (Foo) itemReader.read();
		Assert.state(!foo2.equals(foo1));

		Foo foo3 = (Foo) itemReader.read();
		Assert.state(!foo2.equals(foo3));

		rollback();

		assertEquals(foo2, itemReader.read());
	}

	public void testRetriveZeroKeys() {

		itemReader.setKeyCollector(new KeyCollector<Foo>() {

			public List<Foo> retrieveKeys(ExecutionContext executionContext) {
				return new ArrayList<Foo>();
			}

			public void updateContext(Foo key,
					ExecutionContext executionContext) {
			}
		});

		itemReader.open(new ExecutionContext());
		
		assertNull(itemReader.read());

	}

	private void commit() {
		itemReader.mark();
	}

	private void rollback() {
		itemReader.reset();
	}

	private InitializingBean getAsInitializingBean(ItemReader<Foo> source) {
		return (InitializingBean) source;
	}

	private ItemStream getAsItemStream(ItemReader<Foo> source) {
		return (ItemStream) source;
	}

	private static class MockKeyGenerator implements KeyCollector<Foo> {

		static ExecutionContext streamContext;
		List<Foo> keys;
		List<Foo> restartKeys;
		static final String RESTART_KEY = "restart.keys";

		static {
			// restart data properties cannot be empty.
			streamContext = new ExecutionContext();
			streamContext.put("", "");
		}

		public MockKeyGenerator() {

			keys = new ArrayList<Foo>();
			keys.add(new Foo(1, "1", 1));
			keys.add(new Foo(2, "2", 2));
			keys.add(new Foo(3, "3", 3));
			keys.add(new Foo(4, "4", 4));
			keys.add(new Foo(5, "5", 5));

			restartKeys = new ArrayList<Foo>();
			restartKeys.add(new Foo(3, "3", 3));
			restartKeys.add(new Foo(4, "4", 4));
			restartKeys.add(new Foo(5, "5", 5));
		}

		public ExecutionContext saveState(Object key) {
			return streamContext;
		}

		public List<Foo> retrieveKeys(ExecutionContext executionContext) {
			if (executionContext.containsKey(RESTART_KEY)) {
				return restartKeys;
			} else {
				return keys;
			}
		}

		public void updateContext(Foo key, ExecutionContext executionContext) {
			executionContext.put(RESTART_KEY, restartKeys);
		}

	}

}
