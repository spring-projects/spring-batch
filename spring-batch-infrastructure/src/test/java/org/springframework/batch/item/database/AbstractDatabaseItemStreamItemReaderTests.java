package org.springframework.batch.item.database;

import javax.sql.DataSource;

import org.springframework.batch.item.AbstractItemStreamItemReaderTests;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.junit.Before;
import org.junit.After;

public abstract class AbstractDatabaseItemStreamItemReaderTests extends AbstractItemStreamItemReaderTests {

	private ClassPathXmlApplicationContext ctx;

	@Before
	public void setUp() throws Exception {
		ctx = new ClassPathXmlApplicationContext("org/springframework/batch/item/database/data-source-context.xml");
		super.setUp();
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
		ctx.close();
	}

	protected DataSource getDataSource() {
		return (DataSource) ctx.getBean("dataSource");
	}

}
