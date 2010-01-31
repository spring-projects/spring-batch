package org.springframework.batch.item.database;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.sample.Foo;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Thomas Risberg
 * @author Dave Syer
 */
public abstract class AbstractPagingItemReaderParameterTests {

	protected AbstractPagingItemReader<Foo> tested;
	protected ExecutionContext executionContext = new ExecutionContext();

	@Autowired
	protected DataSource dataSource;

	@Before
	public void setUp() throws Exception {
		tested = getItemReader();
	}

	@After
	public void tearDown() {
		((ItemStream)tested).close();
	}

	@Test
	public void testRead() throws Exception {

		((ItemStream)tested).open(executionContext);

		Foo foo3 = tested.read();
		Assert.assertEquals(3, foo3.getValue());

		Foo foo4 = tested.read();
		Assert.assertEquals(4, foo4.getValue());

		Foo foo5 = tested.read();
		Assert.assertEquals(5, foo5.getValue());

		Object o = tested.read();
		Assert.assertNull(o);
	}

	@Test
	public void testReadAfterJump() throws Exception {

		executionContext.putInt(tested.getClass().getSimpleName()+".read.count", 2);
		((ItemStream)tested).open(executionContext);

		Foo foo5 = tested.read();
		Assert.assertEquals(5, foo5.getValue());

		Object o = tested.read();
		Assert.assertNull(o);
	}

	protected abstract AbstractPagingItemReader<Foo> getItemReader() throws Exception;
}
