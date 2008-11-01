package org.springframework.batch.item.database;

import javax.sql.DataSource;

import org.springframework.batch.item.CommonItemStreamItemReaderTests;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public abstract class CommonDatabaseItemStreamItemReaderTests extends CommonItemStreamItemReaderTests {

	private ClassPathXmlApplicationContext ctx;

	protected void setUp() throws Exception {
		ctx = new ClassPathXmlApplicationContext("org/springframework/batch/item/database/data-source-context.xml");
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		ctx.close();
	}

	protected DataSource getDataSource() {
		return (DataSource) ctx.getBean("dataSource");
	}

}
