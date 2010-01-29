package org.springframework.batch.item.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.sample.Foo;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Common scenarios for testing {@link ItemReader} implementations which read
 * data from database.
 * 
 * @author Lucas Ward
 * @author Robert Kasanicky
 * @author Thomas Risberg
 */
public abstract class AbstractDataSourceItemReaderIntegrationTests {

	protected ItemReader<Foo> reader;

	protected ExecutionContext executionContext;

	protected DataSource dataSource;

	public AbstractDataSourceItemReaderIntegrationTests() {
		super();
	}

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	protected abstract ItemReader<Foo> createItemReader() throws Exception;

	@Before
	public void onSetUpInTransaction() throws Exception {
		reader = createItemReader();
		executionContext = new ExecutionContext();
	}

	@AfterTransaction
	public void onTearDownAfterTransaction() throws Exception {
		getAsItemStream(reader).close();
	}

	/*
	 * Regular scenario - read all rows and eventually return null.
	 */
	@Transactional @Test
	public void testNormalProcessing() throws Exception {
		getAsInitializingBean(reader).afterPropertiesSet();
		getAsItemStream(reader).open(executionContext);
		
		Foo foo1 = reader.read();
		assertEquals(1, foo1.getValue());

		Foo foo2 = reader.read();
		assertEquals(2, foo2.getValue());

		Foo foo3 = reader.read();
		assertEquals(3, foo3.getValue());

		Foo foo4 = reader.read();
		assertEquals(4, foo4.getValue());

		Foo foo5 = reader.read();
		assertEquals(5, foo5.getValue());

		assertNull(reader.read());
	}

	/*
	 * Restart scenario - read records, save restart data, create new input
	 * source and restore from restart data - the new input source should
	 * continue where the old one finished.
	 */
	@Transactional @Test
	public void testRestart() throws Exception {

		getAsItemStream(reader).open(executionContext);
		
		Foo foo1 = reader.read();
		assertEquals(1, foo1.getValue());

		Foo foo2 = reader.read();
		assertEquals(2, foo2.getValue());

		getAsItemStream(reader).update(executionContext);
	
		getAsItemStream(reader).close();

		// create new input source
		reader = createItemReader();

		getAsItemStream(reader).open(executionContext);

		Foo fooAfterRestart = reader.read();
		assertEquals(3, fooAfterRestart.getValue());
	}

	/*
	 * Reading from an input source and then trying to restore causes an error.
	 */
	@Transactional @Test
	public void testInvalidRestore() throws Exception {

		getAsItemStream(reader).open(executionContext);
		
		Foo foo1 = reader.read();
		assertEquals(1, foo1.getValue());

		Foo foo2 = reader.read();
		assertEquals(2, foo2.getValue());

		getAsItemStream(reader).update(executionContext);
	
		getAsItemStream(reader).close();

		// create new input source
		reader = createItemReader();
		getAsItemStream(reader).open(new ExecutionContext());

		Foo foo = reader.read();
		assertEquals(1, foo.getValue());

		try {
			getAsItemStream(reader).open(executionContext);
			fail();
		}
		catch (Exception ex) {
			// expected
		}
	}

	/*
	 * Empty restart data should be handled gracefully.
	 */
	@Transactional @Test
	public void testRestoreFromEmptyData() throws Exception {
		getAsItemStream(reader).open(executionContext);

		Foo foo = reader.read();
		assertEquals(1, foo.getValue());
	}

	/*
	 * Rollback scenario with restart - input source rollbacks to last
	 * commit point.
	 */
	@Transactional @Test
	public void testRollbackAndRestart() throws Exception {

		getAsItemStream(reader).open(executionContext);
		
		Foo foo1 = reader.read();

		getAsItemStream(reader).update(executionContext);

		Foo foo2 = reader.read();
		Assert.state(!foo2.equals(foo1));

		Foo foo3 = reader.read();
		Assert.state(!foo2.equals(foo3));
	
		getAsItemStream(reader).close();

		// create new input source
		reader = createItemReader();

		getAsItemStream(reader).open(executionContext);

		assertEquals(foo2, reader.read());
		assertEquals(foo3, reader.read());
	}
	
	/*
	 * Rollback scenario with restart - input source rollbacks to last
	 * commit point.
	 */
	@Transactional @Test
	public void testRollbackOnFirstChunkAndRestart() throws Exception {

		getAsItemStream(reader).open(executionContext);
		
		Foo foo1 = reader.read();

		Foo foo2 = reader.read();
		Assert.state(!foo2.equals(foo1));

		Foo foo3 = reader.read();
		Assert.state(!foo2.equals(foo3));
	
		getAsItemStream(reader).close();

		// create new input source
		reader = createItemReader();

		getAsItemStream(reader).open(executionContext);

		assertEquals(foo1, reader.read());
		assertEquals(foo2, reader.read());
	}
	
	@Transactional @Test
	public void testMultipleRestarts() throws Exception {
		
		getAsItemStream(reader).open(executionContext);
		

		Foo foo1 = reader.read();

		getAsItemStream(reader).update(executionContext);

		Foo foo2 = reader.read();
		Assert.state(!foo2.equals(foo1));

		Foo foo3 = reader.read();
		Assert.state(!foo2.equals(foo3));
	
		getAsItemStream(reader).close();

		// create new input source
		reader = createItemReader();

		getAsItemStream(reader).open(executionContext);

		assertEquals(foo2, reader.read());
		assertEquals(foo3, reader.read());
		
		getAsItemStream(reader).update(executionContext);
		
		getAsItemStream(reader).close();

		// create new input source
		reader = createItemReader();

		getAsItemStream(reader).open(executionContext);
		
		Foo foo4 = reader.read();
		Foo foo5 = reader.read();
		assertEquals(4, foo4.getValue());
		assertEquals(5, foo5.getValue());
	}

	protected ItemStream getAsItemStream(ItemReader<Foo> source) {
		return (ItemStream) source;
	}

	protected InitializingBean getAsInitializingBean(ItemReader<Foo> source) {
		return (InitializingBean) source;
	}

}