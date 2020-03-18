/*
 * Copyright 2009-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.item.database;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.batch.item.AbstractItemStreamItemReaderTests;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.sample.Foo;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.Assert.assertEquals;

public abstract class AbstractDatabaseItemStreamItemReaderTests extends AbstractItemStreamItemReaderTests {

	protected ClassPathXmlApplicationContext ctx;

    @Override
	@Before
	public void setUp() throws Exception {
		initializeContext();
		super.setUp();
	}

    @Override
	@After
	public void tearDown() throws Exception {
		super.tearDown();
		ctx.close();
	}

	/**
	 * Sub-classes can override this and create their own context.
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
