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
package org.springframework.batch.item;

import static org.junit.Assert.*;

import org.springframework.batch.item.sample.Foo;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

/**
 * Common tests for readers implementing both {@link ItemReader} and
 * {@link ItemStream}. Expected input is five {@link Foo} objects with values 1
 * to 5.
 */
public abstract class AbstractItemStreamItemReaderTests extends AbstractItemReaderTests {

	protected ExecutionContext executionContext = new ExecutionContext();

	/**
	 * Cast the reader to ItemStream.
	 */
	protected ItemStream testedAsStream() {
		return (ItemStream) tested;
	}

    @Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		testedAsStream().open(executionContext);
	}

	@After
	public void tearDown() throws Exception {
		testedAsStream().close();
	}

	/**
	 * Restart scenario - read items, update execution context, create new
	 * reader and restore from restart data - the new input source should
	 * continue where the old one finished.
	 */
	@Test
	public void testRestart() throws Exception {

		testedAsStream().update(executionContext);

		Foo foo1 = tested.read();
		assertEquals(1, foo1.getValue());

		Foo foo2 = tested.read();
		assertEquals(2, foo2.getValue());

		testedAsStream().update(executionContext);

		testedAsStream().close();

		// create new input source
		tested = getItemReader();

		testedAsStream().open(executionContext);

		Foo fooAfterRestart = tested.read();
		assertEquals(3, fooAfterRestart.getValue());
	}

	/**
	 * Restart scenario - read items, rollback to last marked position, update
	 * execution context, create new reader and restore from restart data - the
	 * new input source should continue where the old one finished.
	 */
	@Test
	public void testResetAndRestart() throws Exception {

		testedAsStream().update(executionContext);

		Foo foo1 = tested.read();
		assertEquals(1, foo1.getValue());

		Foo foo2 = tested.read();
		assertEquals(2, foo2.getValue());
		
		testedAsStream().update(executionContext);
		
		Foo foo3 = tested.read();
		assertEquals(3, foo3.getValue());

		testedAsStream().close();

		// create new input source
		tested = getItemReader();

		testedAsStream().open(executionContext);

		Foo fooAfterRestart = tested.read();
		assertEquals(3, fooAfterRestart.getValue());
	}

	@Test
	public void testReopen() throws Exception {
		testedAsStream().update(executionContext);

		Foo foo1 = tested.read();
		assertEquals(1, foo1.getValue());

		Foo foo2 = tested.read();
		assertEquals(2, foo2.getValue());

		testedAsStream().update(executionContext);

		// create new input source
		testedAsStream().close();

		testedAsStream().open(executionContext);

		Foo fooAfterRestart = tested.read();
		assertEquals(3, fooAfterRestart.getValue());
	}

}
