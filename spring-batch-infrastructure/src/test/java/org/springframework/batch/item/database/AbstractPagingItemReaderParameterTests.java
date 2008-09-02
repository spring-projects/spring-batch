package org.springframework.batch.item.database;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.Assert;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.sample.Foo;
import org.springframework.beans.factory.annotation.Autowired;
import javax.sql.DataSource;

/**
 * @author trisberg
 */
public abstract class AbstractPagingItemReaderParameterTests {
	protected ItemReader<Foo> tested;
	protected ExecutionContext executionContext = new ExecutionContext();
	@Autowired
	protected DataSource dataSource;

	@Before
	public void setUp() throws Exception {
		tested = getItemReader();
		((ItemStream)tested).open(executionContext);
	}

	@After
	public void tearDown() {
		((ItemStream)tested).close(executionContext);
	}

	@Test
	public void testRead() throws Exception {

		Foo foo3 = tested.read();
		Assert.assertEquals(3, foo3.getValue());

		Foo foo4 = tested.read();
		Assert.assertEquals(4, foo4.getValue());

		Foo foo5 = tested.read();
		Assert.assertEquals(5, foo5.getValue());

		Object o = tested.read();
		Assert.assertNull(o);
	}

	protected abstract ItemReader<Foo> getItemReader() throws Exception;
}
