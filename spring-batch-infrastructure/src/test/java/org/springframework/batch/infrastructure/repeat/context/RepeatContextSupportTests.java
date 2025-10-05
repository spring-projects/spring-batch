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
package org.springframework.batch.infrastructure.repeat.context;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.repeat.context.RepeatContextSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author dsyer
 *
 */
class RepeatContextSupportTests {

	private final List<String> list = new ArrayList<>();

	/**
	 * Test method for
	 * {@link RepeatContextSupport#registerDestructionCallback(java.lang.String, java.lang.Runnable)}.
	 */
	@Test
	void testDestructionCallbackSunnyDay() {
		RepeatContextSupport context = new RepeatContextSupport(null);
		context.setAttribute("foo", "FOO");
		context.registerDestructionCallback("foo", () -> list.add("bar"));
		context.close();
		assertEquals(1, list.size());
		assertEquals("bar", list.get(0));
	}

	/**
	 * Test method for
	 * {@link RepeatContextSupport#registerDestructionCallback(java.lang.String, java.lang.Runnable)}.
	 */
	@Test
	void testDestructionCallbackMissingAttribute() {
		RepeatContextSupport context = new RepeatContextSupport(null);
		context.registerDestructionCallback("foo", () -> list.add("bar"));
		context.close();
		// No check for the attribute before executing callback
		assertEquals(1, list.size());
	}

	/**
	 * Test method for
	 * {@link RepeatContextSupport#registerDestructionCallback(java.lang.String, java.lang.Runnable)}.
	 */
	@Test
	void testDestructionCallbackWithException() {
		RepeatContextSupport context = new RepeatContextSupport(null);
		context.setAttribute("foo", "FOO");
		context.setAttribute("bar", "BAR");
		context.registerDestructionCallback("bar", () -> {
			list.add("spam");
			throw new RuntimeException("fail!");
		});
		context.registerDestructionCallback("foo", () -> {
			list.add("bar");
			throw new RuntimeException("fail!");
		});
		Exception exception = assertThrows(RuntimeException.class, context::close);
		assertEquals("fail!", exception.getMessage());
		// ...but we do care that both were executed:
		assertEquals(2, list.size());
		assertTrue(list.contains("bar"));
		assertTrue(list.contains("spam"));
	}

}
