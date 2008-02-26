package org.springframework.batch.io.sql;

import org.springframework.batch.io.sample.domain.Foo;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;
import org.springframework.util.Assert;

/**
 * Common scenarios for testing {@link ItemReader} implementations which read data from database.
 *
 * @author Lucas Ward
 * @author Robert Kasanicky
 */
public abstract class AbstractJdbcItemReaderIntegrationTests extends AbstractTransactionalDataSourceSpringContextTests {

	protected ItemReader itemReader;

	protected ExecutionContext executionContext;
	
	/**
	 * @return input source with all necessary dependencies set
	 */
	protected abstract ItemReader createItemReader() throws Exception;

	protected String[] getConfigLocations(){
		return new String[] { "org/springframework/batch/io/sql/data-source-context.xml"};
	}

	protected void onSetUp()throws Exception{
		super.onSetUp();
		itemReader = createItemReader();
		getAsInitializingBean(itemReader).afterPropertiesSet();
		executionContext = new ExecutionContext();
	}

	protected void onTearDown()throws Exception {
		getAsDisposableBean(itemReader).destroy();
		super.onTearDown();
	}

	/**
	 * Regular scenario - read all rows and eventually return null.
	 */
	public void testNormalProcessing() throws Exception {
		getAsInitializingBean(itemReader).afterPropertiesSet();
		getAsItemStream(itemReader).open(executionContext);

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

		getAsItemStream(itemReader).open(executionContext);
		Foo foo1 = (Foo) itemReader.read();
		assertEquals(1, foo1.getValue());

		Foo foo2 = (Foo) itemReader.read();
		assertEquals(2, foo2.getValue());

		getAsItemStream(itemReader).update(executionContext);

		// create new input source
		itemReader = createItemReader();

		Foo foo = (Foo) itemReader.read();
		assertEquals(1, foo.getValue());

		try {
			getAsItemStream(itemReader).open(executionContext);
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
		ExecutionContext streamContext = new ExecutionContext();
		getAsItemStream(itemReader).open(streamContext);
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

	private ItemStream getAsItemStream(ItemReader source) {
		return (ItemStream) source;
	}

	private InitializingBean getAsInitializingBean(ItemReader source) {
		return (InitializingBean) source;
	}

	private DisposableBean getAsDisposableBean(ItemReader source) {
		return (DisposableBean) source;
	}

}
