/*
 * Copyright 2008-2022 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.sample.Foo;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Common scenarios for testing {@link ItemReader} implementations which read data from
 * database.
 *
 * @author Lucas Ward
 * @author Robert Kasanicky
 */
public abstract class AbstractJdbcItemReaderIntegrationTests {

	protected ItemReader<Foo> itemReader;

	protected ExecutionContext executionContext;

	protected abstract ItemReader<Foo> createItemReader() throws Exception;

	protected DataSource dataSource;

	protected JdbcTemplate jdbcTemplate;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@BeforeEach
	void onSetUp() throws Exception {
		itemReader = createItemReader();
		getAsInitializingBean(itemReader).afterPropertiesSet();
		executionContext = new ExecutionContext();
	}

	@AfterEach
	void onTearDown() throws Exception {
		getAsDisposableBean(itemReader).destroy();
	}

	/*
	 * Regular scenario - read all rows and eventually return null.
	 */
	@Transactional
	@Test
	void testNormalProcessing() throws Exception {
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
	@Transactional
	@Test
	void testRestart() throws Exception {
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
	@Transactional
	@Test
	void testInvalidRestore() throws Exception {

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

		assertThrows(IllegalStateException.class, () -> getAsItemStream(itemReader).open(executionContext));
	}

	/*
	 * Empty restart data should be handled gracefully.
	 */
	@Transactional
	@Test
	void testRestoreFromEmptyData() throws Exception {
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
