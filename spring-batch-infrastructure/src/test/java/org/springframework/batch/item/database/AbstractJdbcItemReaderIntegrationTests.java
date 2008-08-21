package org.springframework.batch.item.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.sample.Foo;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Common scenarios for testing {@link ItemReader} implementations which read data from database.
 *
 * @author Lucas Ward
 * @author Robert Kasanicky
 */
public abstract class AbstractJdbcItemReaderIntegrationTests {

	protected ItemReader<Foo> itemReader;

	protected ExecutionContext executionContext;
	
	protected abstract ItemReader<Foo> createItemReader() throws Exception;

	protected DataSource dataSource;

	protected SimpleJdbcTemplate simpleJdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	@Before
	public void onSetUp()throws Exception{
		itemReader = createItemReader();
		getAsInitializingBean(itemReader).afterPropertiesSet();
		executionContext = new ExecutionContext();
	}

	@After
	public void onTearDown()throws Exception {
		getAsDisposableBean(itemReader).destroy();
	}

	/*
	 * Regular scenario - read all rows and eventually return null.
	 */
	@Transactional @Test
	public void testNormalProcessing() throws Exception {
		getAsInitializingBean(itemReader).afterPropertiesSet();
		getAsItemStream(itemReader).open(executionContext);

		Foo foo1 = itemReader.read();
		assertEquals(1, foo1.getValue());

		Foo foo2 = itemReader.read();
		assertEquals(2, foo2.getValue());

		Foo foo3 = itemReader.read();
		assertEquals(3, foo3.getValue());

		Foo foo4 = itemReader.read();
		assertEquals(4, foo4.getValue());

		Foo foo5 = itemReader.read();
		assertEquals(5, foo5.getValue());

		assertNull(itemReader.read());
	}

	/*
	 * Restart scenario.
	 */
	@Transactional @Test
	public void testRestart() throws Exception {
		getAsItemStream(itemReader).open(executionContext);
		Foo foo1 = itemReader.read();
		assertEquals(1, foo1.getValue());

		Foo foo2 = itemReader.read();
		assertEquals(2, foo2.getValue());

		getAsItemStream(itemReader).update(executionContext);

		// create new input source
		itemReader = createItemReader();
		getAsItemStream(itemReader).open(executionContext);

		Foo fooAfterRestart = itemReader.read();
		assertEquals(3, fooAfterRestart.getValue());
	}

	/*
	 * Reading from an input source and then trying to restore causes an error.
	 */
	@Transactional @Test
	public void testInvalidRestore() throws Exception {

		getAsItemStream(itemReader).open(executionContext);
		Foo foo1 = itemReader.read();
		assertEquals(1, foo1.getValue());

		Foo foo2 = itemReader.read();
		assertEquals(2, foo2.getValue());

		getAsItemStream(itemReader).update(executionContext);

		// create new input source
		itemReader = createItemReader();
		getAsItemStream(itemReader).open(new ExecutionContext());

		Foo foo = itemReader.read();
		assertEquals(1, foo.getValue());

		try {
			getAsItemStream(itemReader).open(executionContext);
			fail();
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	/*
	 * Empty restart data should be handled gracefully.
	 */
	@Transactional @Test
	public void testRestoreFromEmptyData() throws Exception {
		ExecutionContext streamContext = new ExecutionContext();
		getAsItemStream(itemReader).open(streamContext);
		Foo foo = itemReader.read();
		assertEquals(1, foo.getValue());
	}

	private ItemStream getAsItemStream(ItemReader<Foo> source) {
		return (ItemStream) source;
	}

	private InitializingBean getAsInitializingBean(ItemReader<Foo> source) {
		return (InitializingBean) source;
	}

	private DisposableBean getAsDisposableBean(ItemReader<Foo> source) {
		return (DisposableBean) source;
	}

}
