/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.step.item;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Dave Syer
 *
 */
public class ItemWrapperTests {

	private Exception exception = new RuntimeException();

	/**
	 * Test method for {@link org.springframework.batch.core.step.item.ItemWrapper#ItemWrapper(java.lang.Object)}.
	 */
	@Test
	public void testItemWrapperT() {
		ItemWrapper<String> wrapper = new ItemWrapper<String>("foo");
		assertEquals("foo", wrapper.getItem());
		assertEquals(null, wrapper.getException());
		assertEquals(0, wrapper.getSkipCount());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.step.item.ItemWrapper#ItemWrapper(java.lang.Object, int)}.
	 */
	@Test
	public void testItemWrapperTInt() {
		ItemWrapper<String> wrapper = new ItemWrapper<String>("foo",2);
		assertEquals("foo", wrapper.getItem());
		assertEquals(null, wrapper.getException());
		assertEquals(2, wrapper.getSkipCount());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.step.item.ItemWrapper#ItemWrapper(java.lang.Object, java.lang.Exception)}.
	 */
	@Test
	public void testItemWrapperTException() {
		ItemWrapper<String> wrapper = new ItemWrapper<String>("foo",exception);
		assertEquals("foo", wrapper.getItem());
		assertEquals(exception, wrapper.getException());
		assertEquals(0, wrapper.getSkipCount());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.step.item.ItemWrapper#ItemWrapper(java.lang.Object, java.lang.Exception, int)}.
	 */
	@Test
	public void testItemWrapperTExceptionInt() {
		ItemWrapper<String> wrapper = new ItemWrapper<String>("foo", exception, 2);
		assertEquals("foo", wrapper.getItem());
		assertEquals(exception , wrapper.getException());
		assertEquals(2, wrapper.getSkipCount());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.step.item.ItemWrapper#toString()}.
	 */
	@Test
	public void testToString() {
		ItemWrapper<String> wrapper = new ItemWrapper<String>("foo");
		assertTrue("foo", wrapper.toString().contains("foo"));
	}

}
