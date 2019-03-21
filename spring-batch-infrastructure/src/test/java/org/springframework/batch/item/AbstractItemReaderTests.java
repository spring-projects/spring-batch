/*
 * Copyright 2009-2010 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.sample.Foo;

/**
 * Common tests for {@link ItemReader} implementations. Expected input is five
 * {@link Foo} objects with values 1 to 5.
 */
public abstract class AbstractItemReaderTests {

	protected ItemReader<Foo> tested;

	/**
	 * @return configured ItemReader ready for use.
	 */
	protected abstract ItemReader<Foo> getItemReader() throws Exception;

	@Before
	public void setUp() throws Exception {
		tested = getItemReader();
	}

	/**
	 * Regular scenario - read the input and eventually return null.
	 */
	@Test
	public void testRead() throws Exception {

		Foo foo1 = tested.read();
		assertEquals(1, foo1.getValue());

		Foo foo2 = tested.read();
		assertEquals(2, foo2.getValue());

		Foo foo3 = tested.read();
		assertEquals(3, foo3.getValue());

		Foo foo4 = tested.read();
		assertEquals(4, foo4.getValue());

		Foo foo5 = tested.read();
		assertEquals(5, foo5.getValue());

		assertNull(tested.read());
	}

	/**
	 * Empty input should be handled gracefully - null is returned on first
	 * read.
	 */
	@Test
	public void testEmptyInput() throws Exception {
		pointToEmptyInput(tested);
		tested.read();
		assertNull(tested.read());
	}

	/**
	 * Point the reader to empty input (close and open if necessary for the new
	 * settings to apply).
	 * 
	 * @param tested
	 *            the reader
	 */
	protected abstract void pointToEmptyInput(ItemReader<Foo> tested)
			throws Exception;

}
