package org.springframework.batch.item.database;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.sample.Foo;
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

	protected ItemReader<Foo> reader;
	protected ExecutionContext executionContext;

	/**
	 * @return configured input source ready for use
	 */
	protected abstract ItemReader<Foo> createItemReader() throws Exception;

	protected String[] getConfigLocations() {
		return new String[] { "org/springframework/batch/item/database/data-source-context.xml" };
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
		getAsItemStream(reader).close(null);
		super.onTearDownAfterTransaction();
	}

	/**
	 * Regular scenario - read all rows and eventually return null.
	 */
	public void testNormalProcessing() throws Exception {
		getAsInitializingBean(reader).afterPropertiesSet();
		getAsItemStream(reader).open(executionContext);
		
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

		getAsItemStream(reader).update(executionContext);

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

		getAsItemStream(reader).update(executionContext);

		// create new input source
		reader = createItemReader();
		getAsItemStream(reader).open(new ExecutionContext());

		Foo foo = (Foo) reader.read();
		assertEquals(1, foo.getValue());

		try {
			getAsItemStream(reader).open(executionContext);
			fail();
		}
		catch (Exception ex) {
			// expected
		}
	}

	/**
	 * Empty restart data should be handled gracefully.
	 * @throws Exception
	 */
	public void testRestoreFromEmptyData() throws Exception {
		getAsItemStream(reader).open(executionContext);

		Foo foo = (Foo) reader.read();
		assertEquals(1, foo.getValue());
	}

	/**
	 * Rollback scenario - input source rollbacks to last commit point.
	 * @throws Exception
	 */
	public void testRollback() throws Exception {
		getAsItemStream(reader).open(executionContext);
		
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
	 * Rollback scenario with restart - input source rollbacks to last
	 * commit point.
	 * @throws Exception
	 */
	public void testRollbackAndRestart() throws Exception {

		getAsItemStream(reader).open(executionContext);
		
		Foo foo1 = (Foo) reader.read();

		commit();

		Foo foo2 = (Foo) reader.read();
		Assert.state(!foo2.equals(foo1));

		Foo foo3 = (Foo) reader.read();
		Assert.state(!foo2.equals(foo3));

		rollback();

		getAsItemStream(reader).update(executionContext);

		// create new input source
		reader = createItemReader();

		getAsItemStream(reader).open(executionContext);

		assertEquals(foo2, reader.read());
		assertEquals(foo3, reader.read());
	}
	
	/**
	 * Rollback scenario with restart - input source rollbacks to last
	 * commit point.
	 * @throws Exception
	 */
	public void testRollbackOnFirstChunkAndRestart() throws Exception {

		getAsItemStream(reader).open(executionContext);
		
		Foo foo1 = (Foo) reader.read();

		Foo foo2 = (Foo) reader.read();
		Assert.state(!foo2.equals(foo1));

		Foo foo3 = (Foo) reader.read();
		Assert.state(!foo2.equals(foo3));

		rollback();

		getAsItemStream(reader).update(executionContext);

		// create new input source
		reader = createItemReader();

		getAsItemStream(reader).open(executionContext);

		assertEquals(foo1, reader.read());
		assertEquals(foo2, reader.read());
	}
	
	public void testMultipleRestarts() throws Exception {
		
		getAsItemStream(reader).open(executionContext);
		
		Foo foo1 = (Foo) reader.read();

		commit();

		Foo foo2 = (Foo) reader.read();
		Assert.state(!foo2.equals(foo1));

		Foo foo3 = (Foo) reader.read();
		Assert.state(!foo2.equals(foo3));

		rollback();

		getAsItemStream(reader).update(executionContext);

		// create new input source
		reader = createItemReader();

		getAsItemStream(reader).open(executionContext);

		assertEquals(foo2, reader.read());
		assertEquals(foo3, reader.read());
		
		getAsItemStream(reader).update(executionContext);
		
		commit();
		
		// create new input source
		reader = createItemReader();

		getAsItemStream(reader).open(executionContext);
		
		Foo foo4 = (Foo)reader.read();
		Foo foo5 = (Foo)reader.read();
		assertEquals(4, foo4.getValue());
		assertEquals(5, foo5.getValue());
	}

	private void commit() {
		reader.mark();
	}

	private void rollback() {
		reader.reset();
	}

	private ItemStream getAsItemStream(ItemReader<Foo> source) {
		return (ItemStream) source;
	}

	private InitializingBean getAsInitializingBean(ItemReader<Foo> source) {
		return (InitializingBean) source;
	}

}
