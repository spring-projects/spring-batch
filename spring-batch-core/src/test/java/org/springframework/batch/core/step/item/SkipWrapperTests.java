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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author Dave Syer
 *
 */
public class SkipWrapperTests {

	private Exception exception = new RuntimeException();

	/**
	 * Test method for {@link SkipWrapper#SkipWrapper(java.lang.Object)}.
	 */
	@Test
	public void testItemWrapperT() {
		SkipWrapper<String> wrapper = new SkipWrapper<String>("foo");
		assertEquals("foo", wrapper.getItem());
		assertEquals(null, wrapper.getException());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.step.item.SkipWrapper#SkipWrapper(java.lang.Object, java.lang.Exception)}.
	 */
	@Test
	public void testItemWrapperTException() {
		SkipWrapper<String> wrapper = new SkipWrapper<String>("foo",exception);
		assertEquals("foo", wrapper.getItem());
		assertEquals(exception, wrapper.getException());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.step.item.SkipWrapper#toString()}.
	 */
	@Test
	public void testToString() {
		SkipWrapper<String> wrapper = new SkipWrapper<String>("foo");
		assertTrue("foo", wrapper.toString().contains("foo"));
	}

}
