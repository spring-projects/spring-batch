/*
 * Copyright 2009-2025 the original author or authors.
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
package org.springframework.batch.infrastructure.item.database;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.AbstractItemStreamItemReaderTests;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemStream;
import org.springframework.batch.infrastructure.item.sample.Foo;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractDatabaseItemStreamItemReaderTests extends AbstractItemStreamItemReaderTests {

	protected ClassPathXmlApplicationContext ctx;

	@Override
	@BeforeEach
	protected void setUp() throws Exception {
		initializeContext();
		super.setUp();
	}

	@Override
	@AfterEach
	protected void tearDown() throws Exception {
		super.tearDown();
		ctx.close();
	}

	/**
	 * Sub-classes can override this and create their own context.
	 */
	protected void initializeContext() throws Exception {
		ctx = new ClassPathXmlApplicationContext("data-source-context.xml");
	}

	@Test
	void testReadToExhaustion() throws Exception {
		ItemReader<Foo> reader = getItemReader();
		((ItemStream) reader).open(new ExecutionContext());
		// pointToEmptyInput(reader);
		int count = 0;
		Foo item = new Foo();
		while (count++ < 100 && item != null) {
			item = reader.read();
		}
		((ItemStream) reader).close();
		assertEquals(7, count);
	}

	protected DataSource getDataSource() {
		return ctx.getBean("dataSource", DataSource.class);
	}

}
