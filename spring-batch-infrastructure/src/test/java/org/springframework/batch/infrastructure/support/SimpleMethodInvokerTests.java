/*
 * Copyright 2002-2023 the original author or authors.
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
package org.springframework.batch.infrastructure.support;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.support.MethodInvoker;
import org.springframework.batch.infrastructure.support.SimpleMethodInvoker;
import org.springframework.util.Assert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Lucas Ward
 * @author Mahmoud Ben Hassine
 *
 */
class SimpleMethodInvokerTests {

	TestClass testClass;

	String value = "foo";

	@BeforeEach
	void setUp() {
		testClass = new TestClass();
	}

	@Test
	void testMethod() throws Exception {

		Method method = TestClass.class.getMethod("before");
		MethodInvoker methodInvoker = new SimpleMethodInvoker(testClass, method);
		methodInvoker.invokeMethod(value);
		assertTrue(testClass.beforeCalled);
	}

	@Test
	void testMethodByName() {

		MethodInvoker methodInvoker = new SimpleMethodInvoker(testClass, "before", String.class);
		methodInvoker.invokeMethod(value);
		assertTrue(testClass.beforeCalled);
	}

	@Test
	void testMethodWithExecution() throws Exception {
		Method method = TestClass.class.getMethod("beforeWithArgument", String.class);
		MethodInvoker methodInvoker = new SimpleMethodInvoker(testClass, method);
		methodInvoker.invokeMethod(value);
		assertTrue(testClass.beforeCalled);
	}

	@Test
	void testMethodByNameWithExecution() {
		MethodInvoker methodInvoker = new SimpleMethodInvoker(testClass, "beforeWithArgument", String.class);
		methodInvoker.invokeMethod(value);
		assertTrue(testClass.beforeCalled);
	}

	@Test
	void testMethodWithTooManyArguments() throws Exception {
		Method method = TestClass.class.getMethod("beforeWithTooManyArguments", String.class, int.class);
		MethodInvoker methodInvoker = new SimpleMethodInvoker(testClass, method);
		assertThrows(IllegalArgumentException.class, () -> methodInvoker.invokeMethod(value));
		assertFalse(testClass.beforeCalled);
	}

	@Test
	void testMethodByNameWithTooManyArguments() {
		assertThrows(IllegalArgumentException.class,
				() -> new SimpleMethodInvoker(testClass, "beforeWithTooManyArguments", String.class));
	}

	@Test
	void testMethodWithArgument() {
		MethodInvoker methodInvoker = new SimpleMethodInvoker(testClass, "argumentTest", Object.class);
		methodInvoker.invokeMethod(new Object());
		assertTrue(testClass.argumentTestCalled);
	}

	@Test
	void testEquals() throws Exception {
		Method method = TestClass.class.getMethod("beforeWithArgument", String.class);
		MethodInvoker methodInvoker = new SimpleMethodInvoker(testClass, method);

		method = TestClass.class.getMethod("beforeWithArgument", String.class);
		MethodInvoker methodInvoker2 = new SimpleMethodInvoker(testClass, method);
		assertEquals(methodInvoker, methodInvoker2);
	}

	@SuppressWarnings("unused")
	private static class TestClass {

		boolean beforeCalled = false;

		boolean argumentTestCalled = false;

		public void before() {
			beforeCalled = true;
		}

		public void beforeWithArgument(String value) {
			beforeCalled = true;
		}

		public void beforeWithTooManyArguments(String value, int someInt) {
			beforeCalled = true;
		}

		public void argumentTest(Object object) {
			Assert.notNull(object, "Object must not be null");
			argumentTestCalled = true;
		}

	}

}
