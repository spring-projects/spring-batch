package org.springframework.batch.io.sql;

import org.springframework.batch.io.sample.domain.Foo;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ExecutionAttributes;
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

	protected ItemReader source;


	/**
	 * @return input source with all necessary dependencies set
	 */
	protected abstract ItemReader createItemReader() throws Exception;

	protected String[] getConfigLocations(){
		return new String[] { "org/springframework/batch/io/sql/data-source-context.xml"};
	}

	protected void onSetUp()throws Exception{
		super.onSetUp();
		source = createItemReader();
		getAsInitializingBean(source).afterPropertiesSet();
	}

	protected void onTearDown()throws Exception {
		getAsDisposableBean(source).destroy();
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
	 * Restart scenario.
	 * @throws Exception
	 */
	public void testRestart() throws Exception {

		Foo foo1 = (Foo) source.read();
		assertEquals(1, foo1.getValue());

		Foo foo2 = (Foo) source.read();
		assertEquals(2, foo2.getValue());

		ExecutionAttributes streamContext = getAsRestartable(source).getStreamContext();

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

		ExecutionAttributes streamContext = getAsRestartable(source).getStreamContext();

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
		ExecutionAttributes streamContext = new ExecutionAttributes();

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
		((ItemStream) source).mark(null);
	}

	private void rollback() {
		((ItemStream) source).reset(null);
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
