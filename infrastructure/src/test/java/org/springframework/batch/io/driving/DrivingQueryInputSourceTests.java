package org.springframework.batch.io.driving;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.springframework.batch.io.InputSource;
import org.springframework.batch.io.sample.domain.Foo;
import org.springframework.batch.restart.GenericRestartData;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.restart.Restartable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.util.Assert;

public class DrivingQueryInputSourceTests extends TestCase {
	
	InputSource source;
	
	static {
		TransactionSynchronizationManager.initSynchronization();
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		
		source = createInputSource();
	}
	
	private InputSource createInputSource() throws Exception{
		
		DrivingQueryInputSource inputSource = new DrivingQueryInputSource();
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

		RestartData restartData = getAsRestartable(source).getRestartData();

		// create new input source
		source = createInputSource();

		getAsRestartable(source).restoreFrom(restartData);

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

		RestartData restartData = getAsRestartable(source).getRestartData();

		// create new input source
		source = createInputSource();

		Foo foo = (Foo) source.read();
		assertEquals(1, foo.getValue());

		try {
			getAsRestartable(source).restoreFrom(restartData);
			fail();
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	/**
	 * Empty restart data should be handled gracefully.
	 */
	public void testRestoreFromEmptyData() {
		RestartData restartData = new GenericRestartData(new Properties());

		getAsRestartable(source).restoreFrom(restartData);

		Foo foo = (Foo) source.read();
		assertEquals(1, foo.getValue());
	}

	/**
	 * Rollback scenario.
	 */
	public void testRollback() {
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
	
	private InitializingBean getAsInitializingBean(InputSource source) {
		return (InitializingBean) source;
	}

	private Restartable getAsRestartable(InputSource source) {
		return (Restartable) source;
	}
	
	private static class MockKeyGenerator implements KeyGenerator{

		static RestartData restartData;
		List keys;
		List restartKeys;
		
		static{
			Properties props = new Properties();
			//restart data properties cannot be empty.
			props.setProperty("", "");
			
			restartData = new GenericRestartData(props);
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
		
		public RestartData getKeyAsRestartData(Object key) {
			return restartData;
		}

		public List restoreKeys(RestartData restartData) {
			
			assertEquals(this.restartData, restartData);
			return restartKeys;
		}

		public List retrieveKeys() {
			return keys;
		}
		
	}

}
