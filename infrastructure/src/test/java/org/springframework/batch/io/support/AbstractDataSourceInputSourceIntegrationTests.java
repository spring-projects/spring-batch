package org.springframework.batch.io.support;

import java.util.Properties;

import org.springframework.batch.io.InputSource;
import org.springframework.batch.io.sample.domain.Foo;
import org.springframework.batch.repeat.synch.BatchTransactionSynchronizationManager;
import org.springframework.batch.restart.GenericRestartData;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.restart.Restartable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.util.Assert;

/**
 * Common scenarios for testing {@link InputSource} implementations which read data from database.
 * 
 * @author Lucas Ward
 * @author Robert Kasanicky
 */
public abstract class AbstractDataSourceInputSourceIntegrationTests extends AbstractTransactionalDataSourceSpringContextTests {

	protected InputSource source;
	

	/**
	 * @return configured input source ready for use
	 */
	protected abstract InputSource createInputSource() throws Exception;
	
	protected String[] getConfigLocations(){
		return new String[] { "org/springframework/batch/io/sql/data-source-context.xml"};
	}
	
	protected void onSetUp()throws Exception{
		super.onSetUp();
		BatchTransactionSynchronizationManager.clearSynchronizations();
		source = createInputSource();
	}
	
	protected void onTearDown()throws Exception {
		getAsDisposableBean(source).destroy();
		BatchTransactionSynchronizationManager.clearSynchronizations();
		super.onTearDown();
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
	 * Restart scenario - read records, save restart data, create new input source
	 * and restore from restart data - the new input source should continue where
	 * the old one finished.
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
	 * Rollback scenario - input source rollbacks to last commit point.
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
	
	private Restartable getAsRestartable(InputSource source) {
		return (Restartable) source;
	}
	
	private InitializingBean getAsInitializingBean(InputSource source) {
		return (InitializingBean) source;
	}
	
	private DisposableBean getAsDisposableBean(InputSource source) {
		return (DisposableBean) source;
	}

}
