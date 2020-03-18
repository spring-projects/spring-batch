/*
 * Copyright 2008-2014 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.sample.Foo;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
	@Test
	@Transactional
	@DirtiesContext
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
	@Test
	@Transactional
	@DirtiesContext
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
	 * Restart scenario - read records, save restart data, create new input
	 * source and restore from restart data - the new input source should
	 * continue where the old one finished.
	 */
	@Test
	@Transactional
	@DirtiesContext
	public void testRestartOnSecondPage() throws Exception {

		getAsItemStream(reader).open(executionContext);
		
		Foo foo1 = reader.read();
		assertEquals(1, foo1.getValue());
		Foo foo2 = reader.read();
		assertEquals(2, foo2.getValue());
		Foo foo3 = reader.read();
		assertEquals(3, foo3.getValue());
		Foo foo4 = reader.read();
		assertEquals(4, foo4.getValue());

		getAsItemStream(reader).update(executionContext);
	
		getAsItemStream(reader).close();

		// create new input source
		reader = createItemReader();

		getAsItemStream(reader).open(executionContext);

		Foo foo5 = reader.read();
		assertEquals(5, foo5.getValue());

		assertNull(reader.read());
	}

	/*
	 * Reading from an input source and then trying to restore causes an error.
	 */
	@Test
	@Transactional
	@DirtiesContext
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
	@Test
	@Transactional
	@DirtiesContext
	public void testRestoreFromEmptyData() throws Exception {
		getAsItemStream(reader).open(executionContext);

		Foo foo = reader.read();
		assertEquals(1, foo.getValue());
	}

	/*
	 * Rollback scenario with restart - input source rollbacks to last
	 * commit point.
	 */
	@Test
	@Transactional
	@DirtiesContext
	public void testRollbackAndRestart() throws Exception {

		getAsItemStream(reader).open(executionContext);
		
		Foo foo1 = reader.read();

		getAsItemStream(reader).update(executionContext);

		Foo foo2 = reader.read();
		assertTrue(!foo2.equals(foo1));

		Foo foo3 = reader.read();
		assertTrue(!foo2.equals(foo3));
	
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
	@Test
	@Transactional
	@DirtiesContext
	public void testRollbackOnFirstChunkAndRestart() throws Exception {

		getAsItemStream(reader).open(executionContext);
		
		Foo foo1 = reader.read();

		Foo foo2 = reader.read();
		assertTrue(!foo2.equals(foo1));

		Foo foo3 = reader.read();
		assertTrue(!foo2.equals(foo3));
	
		getAsItemStream(reader).close();

		// create new input source
		reader = createItemReader();

		getAsItemStream(reader).open(executionContext);

		assertEquals(foo1, reader.read());
		assertEquals(foo2, reader.read());
	}

	@Test
	@Transactional
	@DirtiesContext
	public void testMultipleRestarts() throws Exception {
		
		getAsItemStream(reader).open(executionContext);
		

		Foo foo1 = reader.read();

		getAsItemStream(reader).update(executionContext);

		Foo foo2 = reader.read();
		assertTrue(!foo2.equals(foo1));

		Foo foo3 = reader.read();
		assertTrue(!foo2.equals(foo3));
	
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
	
	//set transaction to false and make sure the tests work
	@Test
	@DirtiesContext
	public void testTransacted() throws Exception {
		if (reader instanceof JpaPagingItemReader) {
			((JpaPagingItemReader<Foo>)reader).setTransacted(false);
			this.testNormalProcessing();
		}//end if
	}

	protected ItemStream getAsItemStream(ItemReader<Foo> source) {
		return (ItemStream) source;
	}

	protected InitializingBean getAsInitializingBean(ItemReader<Foo> source) {
		return (InitializingBean) source;
	}

}