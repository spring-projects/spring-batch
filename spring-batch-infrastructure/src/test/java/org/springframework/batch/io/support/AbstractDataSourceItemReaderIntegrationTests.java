package org.springframework.batch.io.support;

import java.util.Properties;

import org.springframework.batch.io.Skippable;
import org.springframework.batch.io.sample.domain.Foo;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.StreamContext;
import org.springframework.batch.item.stream.GenericStreamContext;
import org.springframework.batch.repeat.synch.BatchTransactionSynchronizationManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.util.Assert;

/**
 * Common scenarios for testing {@link ItemReader} implementations which read data from database.
 * 
 * @author Lucas Ward
 * @author Robert Kasanicky
 */
public abstract class AbstractDataSourceItemReaderIntegrationTests extends AbstractTransactionalDataSourceSpringContextTests {

	protected ItemReader source;
	

	/**
	 * @return configured input source ready for use
	 */
	protected abstract ItemReader createItemReader() throws Exception;
	
	protected String[] getConfigLocations(){
		return new String[] { "org/springframework/batch/io/sql/data-source-context.xml"};
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.test.AbstractTransactionalSpringContextTests#onSetUpInTransaction()
	 */
	protected void onSetUpInTransaction() throws Exception {
		super.onSetUpInTransaction();
		BatchTransactionSynchronizationManager.clearSynchronizations();
		source = createItemReader();
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.test.AbstractTransactionalSpringContextTests#onTearDownAfterTransaction()
	 */
	protected void onTearDownAfterTransaction() throws Exception {
		getAsDisposableBean(source).destroy();
		BatchTransactionSynchronizationManager.clearSynchronizations();
		super.onTearDownAfterTransaction();
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

		StreamContext streamContext = getAsRestartable(source).getRestartData();

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

		StreamContext streamContext = getAsRestartable(source).getRestartData();

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
	 * Rollback scenario - input source rollbacks to last commit point.
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
	
	/**
	 * Rollback scenario with skip - input source rollbacks to last commit point.
	 * @throws Exception 
	 */
	public void testRollbackAndSkip() throws Exception {
		
		if (!(source instanceof Skippable)) {
			return;
		}
		
		Foo foo1 = (Foo) source.read();
		
		commit();
		
		Foo foo2 = (Foo) source.read();
		Assert.state(!foo2.equals(foo1));
		
		Foo foo3 = (Foo) source.read();
		Assert.state(!foo2.equals(foo3));
		
		getAsSkippable(source).skip();
		
		rollback();
		
		assertEquals(foo2, source.read());
		Foo foo4 = (Foo) source.read();
		assertEquals(4, foo4.getValue());
	}

	/**
	 * Rollback scenario with skip and restart - input source rollbacks to last commit point.
	 * @throws Exception 
	 */
	public void testRollbackSkipAndRestart() throws Exception {

		if (!(source instanceof Skippable)) {
			return;
		}

		Foo foo1 = (Foo) source.read();
		
		commit();
		
		Foo foo2 = (Foo) source.read();
		Assert.state(!foo2.equals(foo1));
		
		Foo foo3 = (Foo) source.read();
		Assert.state(!foo2.equals(foo3));
		
		getAsSkippable(source).skip();
		
		rollback();
		
		StreamContext streamContext = getAsRestartable(source).getRestartData();

		// create new input source
		source = createItemReader();

		getAsRestartable(source).restoreFrom(streamContext);

		assertEquals(foo2, source.read());
		Foo foo4 = (Foo) source.read();
		assertEquals(4, foo4.getValue());
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
	
	private Skippable getAsSkippable(ItemReader source) {
		return (Skippable) source;
	}
	
	private ItemStream getAsRestartable(ItemReader source) {
		return (ItemStream) source;
	}

	private InitializingBean getAsInitializingBean(ItemReader source) {
		return (InitializingBean) source;
	}
	
	private DisposableBean getAsDisposableBean(ItemReader source) {
		return (DisposableBean) source;
	}

}
