package org.springframework.batch.item.database;

import static org.junit.Assert.assertEquals;

import javax.sql.DataSource;

import org.springframework.batch.item.AbstractItemStreamItemReaderTests;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.sample.Foo;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

public abstract class AbstractDatabaseItemStreamItemReaderTests extends AbstractItemStreamItemReaderTests {

	protected ClassPathXmlApplicationContext ctx;

	@Before
	public void setUp() throws Exception {
		initializeContext();
		super.setUp();
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
		ctx.close();
	}

	/**
	 * Sub-classes can override this and create their own context.
	 * @throws Exception
	 */
	protected void initializeContext() throws Exception {
		ctx = new ClassPathXmlApplicationContext("org/springframework/batch/item/database/data-source-context.xml");
	}

	@Test
	public void testReadToExhaustion() throws Exception {
		ItemReader<Foo> reader = getItemReader();
		((ItemStream) reader).open(new ExecutionContext());
		// pointToEmptyInput(reader);
		int count = 0;
		Foo item = new Foo();
		while (count++<100 && item!=null) {
			item = reader.read();
		}
		((ItemStream) reader).close();
		assertEquals(7, count);
	}

	protected DataSource getDataSource() {
		return (DataSource) ctx.getBean("dataSource");
	}

}
