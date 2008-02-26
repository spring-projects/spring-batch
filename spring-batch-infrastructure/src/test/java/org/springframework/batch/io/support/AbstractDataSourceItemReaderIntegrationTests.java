package org.springframework.batch.io.support;

import org.springframework.batch.io.Skippable;
import org.springframework.batch.io.sample.domain.Foo;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;
import org.springframework.util.Assert;

/**
 * Common scenarios for testing {@link ItemReader} implementations which read
 * data from database.
 * 
 * @author Lucas Ward
 * @author Robert Kasanicky
 */
public abstract class AbstractDataSourceItemReaderIntegrationTests extends
		AbstractTransactionalDataSourceSpringContextTests {

	protected ItemReader reader;
	protected ExecutionContext executionContext;

	/**
	 * @return configured input source ready for use
	 */
	protected abstract ItemReader createItemReader() throws Exception;

	protected String[] getConfigLocations() {
		return new String[] { "org/springframework/batch/io/sql/data-source-context.xml" };
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.test.AbstractTransactionalSpringContextTests#onSetUpInTransaction()
	 */
	protected void onSetUpInTransaction() throws Exception {
		super.onSetUpInTransaction();
		reader = createItemReader();
		executionContext = new ExecutionContext();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.test.AbstractTransactionalSpringContextTests#onTearDownAfterTransaction()
	 */
	protected void onTearDownAfterTransaction() throws Exception {
		getAsItemStream(reader).close();
		super.onTearDownAfterTransaction();
	}

	/**
	 * Regular scenario - read all rows and eventually return null.
	 */
	public void testNormalProcessing() throws Exception {
		getAsInitializingBean(reader).afterPropertiesSet();

		Foo foo1 = (Foo) reader.read();
		assertEquals(1, foo1.getValue());

		Foo foo2 = (Foo) reader.read();
		assertEquals(2, foo2.getValue());

		Foo foo3 = (Foo) reader.read();
		assertEquals(3, foo3.getValue());

		Foo foo4 = (Foo) reader.read();
		assertEquals(4, foo4.getValue());

		Foo foo5 = (Foo) reader.read();
		assertEquals(5, foo5.getValue());

		assertNull(reader.read());
	}

	/**
	 * Restart scenario - read records, save restart data, create new input
	 * source and restore from restart data - the new input source should
	 * continue where the old one finished.
	 */
	public void testRestart() throws Exception {

		getAsItemStream(reader).open(executionContext);
		
		Foo foo1 = (Foo) reader.read();
		assertEquals(1, foo1.getValue());

		Foo foo2 = (Foo) reader.read();
		assertEquals(2, foo2.getValue());

		getAsItemStream(reader).update();

		// create new input source
		reader = createItemReader();

		getAsItemStream(reader).open(executionContext);

		Foo fooAfterRestart = (Foo) reader.read();
		assertEquals(3, fooAfterRestart.getValue());
	}

	/**
	 * Reading from an input source and then trying to restore causes an error.
	 */
	public void testInvalidRestore() throws Exception {

		getAsItemStream(reader).open(executionContext);
		
		Foo foo1 = (Foo) reader.read();
		assertEquals(1, foo1.getValue());

		Foo foo2 = (Foo) reader.read();
		assertEquals(2, foo2.getValue());

		getAsItemStream(reader).update();

		// create new input source
		reader = createItemReader();

		Foo foo = (Foo) reader.read();
		assertEquals(1, foo.getValue());

		try {
			getAsItemStream(reader).open(executionContext);
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
		getAsItemStream(reader).update();

		Foo foo = (Foo) reader.read();
		assertEquals(1, foo.getValue());
	}

	/**
	 * Rollback scenario - input source rollbacks to last commit point.
	 * @throws Exception
	 */
	public void testRollback() throws Exception {
		Foo foo1 = (Foo) reader.read();

		commit();

		Foo foo2 = (Foo) reader.read();
		Assert.state(!foo2.equals(foo1));

		Foo foo3 = (Foo) reader.read();
		Assert.state(!foo2.equals(foo3));

		rollback();

		assertEquals(foo2, reader.read());
	}

	/**
	 * Rollback scenario with skip - input source rollbacks to last commit
	 * point.
	 * @throws Exception
	 */
	public void testRollbackAndSkip() throws Exception {

		if (!(reader instanceof Skippable)) {
			return;
		}

		Foo foo1 = (Foo) reader.read();

		commit();

		Foo foo2 = (Foo) reader.read();
		Assert.state(!foo2.equals(foo1));

		Foo foo3 = (Foo) reader.read();
		Assert.state(!foo2.equals(foo3));

		getAsSkippable(reader).skip();

		rollback();

		assertEquals(foo2, reader.read());
		Foo foo4 = (Foo) reader.read();
		assertEquals(4, foo4.getValue());
	}

	/**
	 * Rollback scenario with skip and restart - input source rollbacks to last
	 * commit point.
	 * @throws Exception
	 */
	public void testRollbackSkipAndRestart() throws Exception {

		if (!(reader instanceof Skippable)) {
			return;
		}

		getAsItemStream(reader).open(executionContext);
		
		Foo foo1 = (Foo) reader.read();

		commit();

		Foo foo2 = (Foo) reader.read();
		Assert.state(!foo2.equals(foo1));

		Foo foo3 = (Foo) reader.read();
		Assert.state(!foo2.equals(foo3));

		getAsSkippable(reader).skip();

		rollback();

		getAsItemStream(reader).update();

		// create new input source
		reader = createItemReader();

		getAsItemStream(reader).open(executionContext);

		assertEquals(foo2, reader.read());
		Foo foo4 = (Foo) reader.read();
		assertEquals(4, foo4.getValue());
	}

	private void commit() {
		reader.mark();
	}

	private void rollback() {
		reader.reset();
	}

	private Skippable getAsSkippable(ItemReader source) {
		return (Skippable) source;
	}

	private ItemStream getAsItemStream(ItemReader source) {
		return (ItemStream) source;
	}

	private InitializingBean getAsInitializingBean(ItemReader source) {
		return (InitializingBean) source;
	}

}
