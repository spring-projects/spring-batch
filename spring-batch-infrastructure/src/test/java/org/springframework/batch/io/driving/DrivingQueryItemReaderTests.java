package org.springframework.batch.io.driving;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.springframework.batch.io.sample.domain.Foo;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.StreamContext;
import org.springframework.batch.item.stream.GenericStreamContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.util.Assert;

public class DrivingQueryItemReaderTests extends TestCase {
	
	ItemReader source;
	
	static {
		TransactionSynchronizationManager.initSynchronization();
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		
		source = createItemReader();
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
		getAsInitializingBean(source).afterPropertiesSet();

		Foo foo1 = (Foo) source.read();
		assertEquals(1, foo1.getValue());

		Foo foo2 = (Foo) source.read();
		assertEquals(2, foo2.getValue());

		Foo foo3 = (Foo) source.read();
		assertEquals(3, foo3.getValue());

		Foo foo4 = (Foo) source.read();
		assertEquals(4, foo4.getValue());

		Foo foo5 = (Foo) source.read();
		assertEquals(5, foo5.getValue());

		assertNull(source.read());
	}

	/**
	 * Restart scenario.
	 * @throws Exception
	 */
	public void testRestart() throws Exception {

		Foo foo1 = (Foo) source.read();
		assertEquals(1, foo1.getValue());

		Foo foo2 = (Foo) source.read();
		assertEquals(2, foo2.getValue());

		StreamContext streamContext = getAsRestartable(source).getStreamContext();

		// create new input source
		source = createItemReader();

		getAsRestartable(source).restoreFrom(streamContext);

		Foo fooAfterRestart = (Foo) source.read();
		assertEquals(3, fooAfterRestart.getValue());
	}

	/**
	 * Reading from an input source and then trying to restore causes an error.
	 */
	public void testInvalidRestore() throws Exception {

		Foo foo1 = (Foo) source.read();
		assertEquals(1, foo1.getValue());

		Foo foo2 = (Foo) source.read();
		assertEquals(2, foo2.getValue());

		StreamContext streamContext = getAsRestartable(source).getStreamContext();

		// create new input source
		source = createItemReader();

		Foo foo = (Foo) source.read();
		assertEquals(1, foo.getValue());

		try {
			getAsRestartable(source).restoreFrom(streamContext);
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
		StreamContext streamContext = new GenericStreamContext(new Properties());

		getAsRestartable(source).restoreFrom(streamContext);

		Foo foo = (Foo) source.read();
		assertEquals(1, foo.getValue());
	}

	/**
	 * Rollback scenario.
	 * @throws Exception 
	 */
	public void testRollback() throws Exception {
		Foo foo1 = (Foo) source.read();

		commit();

		Foo foo2 = (Foo) source.read();
		Assert.state(!foo2.equals(foo1));

		Foo foo3 = (Foo) source.read();
		Assert.state(!foo2.equals(foo3));

		rollback();

		assertEquals(foo2, source.read());
	}


	private void commit() {
		TransactionSynchronizationUtils.invokeAfterCompletion(
				TransactionSynchronizationManager.getSynchronizations(),
				TransactionSynchronization.STATUS_COMMITTED);
	}

	private void rollback() {
		TransactionSynchronizationUtils.invokeAfterCompletion(
				TransactionSynchronizationManager.getSynchronizations(),
				TransactionSynchronization.STATUS_ROLLED_BACK);
	}
	
	private InitializingBean getAsInitializingBean(ItemReader source) {
		return (InitializingBean) source;
	}

	private ItemStream getAsRestartable(ItemReader source) {
		return (ItemStream) source;
	}
	
	private static class MockKeyGenerator implements KeyGenerator{

		static StreamContext streamContext;
		List keys;
		List restartKeys;
		
		static{
			Properties props = new Properties();
			//restart data properties cannot be empty.
			props.setProperty("", "");
			
			streamContext = new GenericStreamContext(props);
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
		
		public StreamContext getKeyAsStreamContext(Object key) {
			return streamContext;
		}

		public List restoreKeys(StreamContext streamContext) {
			
			assertEquals(MockKeyGenerator.streamContext, streamContext);
			return restartKeys;
		}

		public List retrieveKeys() {
			return keys;
		}
		
	}

}
