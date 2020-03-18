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

		Foo foo2 = tested.read();
		Assert.assertEquals(2, foo2.getValue());

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
	public void testReadAfterJumpFirstPage() throws Exception {

		executionContext.putInt(getName()+".read.count", 2);
		((ItemStream)tested).open(executionContext);

		Foo foo4 = tested.read();
		Assert.assertEquals(4, foo4.getValue());

		Foo foo5 = tested.read();
		Assert.assertEquals(5, foo5.getValue());

		Object o = tested.read();
		Assert.assertNull(o);
	}

	@Test
	public void testReadAfterJumpSecondPage() throws Exception {

		executionContext.putInt(getName()+".read.count", 3);
		((ItemStream)tested).open(executionContext);

		Foo foo5 = tested.read();
		Assert.assertEquals(5, foo5.getValue());

		Object o = tested.read();
		Assert.assertNull(o);
	}
	
	protected String getName() {
		return tested.getClass().getSimpleName();
	}
	
	protected abstract AbstractPagingItemReader<Foo> getItemReader() throws Exception;
}
