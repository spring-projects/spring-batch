/*
 * Copyright 2006-2022 the original author or authors.
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
package org.springframework.batch.core.step.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.SkipWrapper;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class SkipWrapperTests {

	private final Exception exception = new RuntimeException();

	/**
	 * Test method for {@link SkipWrapper#SkipWrapper(java.lang.Object)}.
	 */
	@Test
	void testItemWrapperT() {
		SkipWrapper<String> wrapper = new SkipWrapper<>("foo");
		assertEquals("foo", wrapper.getItem());
		assertNull(wrapper.getException());
	}

	/**
	 * Test method for
	 * {@link SkipWrapper#SkipWrapper(java.lang.Object, java.lang.Throwable)}.
	 */
	@Test
	void testItemWrapperTException() {
		SkipWrapper<String> wrapper = new SkipWrapper<>("foo", exception);
		assertEquals("foo", wrapper.getItem());
		assertEquals(exception, wrapper.getException());
	}

	/**
	 * Test method for {@link SkipWrapper#toString()}.
	 */
	@Test
	void testToString() {
		SkipWrapper<String> wrapper = new SkipWrapper<>("foo");
		assertTrue(wrapper.toString().contains("foo"), "foo");
	}

}
