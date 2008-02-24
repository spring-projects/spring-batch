package org.springframework.batch.io.driving;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.springframework.batch.io.sample.domain.Foo;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

public class DrivingQueryItemReaderTests extends TestCase {
	
	ItemReader itemReader;
	
	static {
		TransactionSynchronizationManager.initSynchronization();
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		
		itemReader = createItemReader();
	}
	
	private ItemReader createItemReader() throws Exception{
		
		DrivingQueryItemReader inputSource = new DrivingQueryItemReader();
		inputSource.setKeyGenerator(new MockKeyGenerator());
		
		return inputSource;
	}
	
	
	/**
	 * Regular scenario - read all rows and eventually return null.
	 */
	public void testNormalProcessing() throws Exception {
		getAsInitializingBean(itemReader).afterPropertiesSet();

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
	 * @throws Exception
	 */
	public void testRestart() throws Exception {

		Foo foo1 = (Foo) itemReader.read();
		assertEquals(1, foo1.getValue());

		Foo foo2 = (Foo) itemReader.read();
		assertEquals(2, foo2.getValue());

		ExecutionContext streamContext = getAsRestartable(itemReader).getExecutionContext();

		// create new input source
		itemReader = createItemReader();

		getAsRestartable(itemReader).restoreFrom(streamContext);

		Foo fooAfterRestart = (Foo) itemReader.read();
		assertEquals(3, fooAfterRestart.getValue());
	}

	/**
	 * Reading from an input source and then trying to restore causes an error.
	 */
	public void testInvalidRestore() throws Exception {

		Foo foo1 = (Foo) itemReader.read();
		assertEquals(1, foo1.getValue());

		Foo foo2 = (Foo) itemReader.read();
		assertEquals(2, foo2.getValue());

		ExecutionContext streamContext = getAsRestartable(itemReader).getExecutionContext();

		// create new input source
		itemReader = createItemReader();

		Foo foo = (Foo) itemReader.read();
		assertEquals(1, foo.getValue());

		try {
			getAsRestartable(itemReader).restoreFrom(streamContext);
			fail();
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	/**
	 * Empty restart data should be handled gracefully.
	 * @throws Exception 
	 */
	public void testRestoreFromEmptyData() throws Exception {
		ExecutionContext streamContext = new ExecutionContext(new Properties());

		getAsRestartable(itemReader).restoreFrom(streamContext);

		Foo foo = (Foo) itemReader.read();
		assertEquals(1, foo.getValue());
	}

	/**
	 * Rollback scenario.
	 * @throws Exception 
	 */
	public void testRollback() throws Exception {
		Foo foo1 = (Foo) itemReader.read();

		commit();

		Foo foo2 = (Foo) itemReader.read();
		Assert.state(!foo2.equals(foo1));

		Foo foo3 = (Foo) itemReader.read();
		Assert.state(!foo2.equals(foo3));

		rollback();

		assertEquals(foo2, itemReader.read());
	}


	private void commit() {
		itemReader.mark();
	}

	private void rollback() {
		itemReader.reset();
	}
	
	private InitializingBean getAsInitializingBean(ItemReader source) {
		return (InitializingBean) source;
	}

	private ItemStream getAsRestartable(ItemReader source) {
		return (ItemStream) source;
	}
	
	private static class MockKeyGenerator implements KeyGenerator{

		static ExecutionContext streamContext;
		List keys;
		List restartKeys;
		
		static{
			Properties props = new Properties();
			//restart data properties cannot be empty.
			props.setProperty("", "");
			
			streamContext = new ExecutionContext(props);
		}
		
		public MockKeyGenerator() {
			
			keys = new ArrayList();
			keys.add(new Foo(1, "1", 1));
			keys.add(new Foo(2, "2", 2));
			keys.add(new Foo(3, "3", 3));
			keys.add(new Foo(4, "4", 4));
			keys.add(new Foo(5, "5", 5));
			
			restartKeys = new ArrayList();
			restartKeys.add(new Foo(3, "3", 3));
			restartKeys.add(new Foo(4, "4", 4));
			restartKeys.add(new Foo(5, "5", 5));
		}
		
		public ExecutionContext getKeyAsExecutionContext(Object key) {
			return streamContext;
		}

		public List restoreKeys(ExecutionContext streamContext) {
			
			assertEquals(MockKeyGenerator.streamContext, streamContext);
			return restartKeys;
		}

		public List retrieveKeys() {
			return keys;
		}
		
	}

}
