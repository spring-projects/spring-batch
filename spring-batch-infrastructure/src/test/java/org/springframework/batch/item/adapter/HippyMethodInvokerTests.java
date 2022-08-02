/*
 * Copyright 2010-2022 the original author or authors.
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
package org.springframework.batch.item.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

class HippyMethodInvokerTests {

	@Test
	void testVanillaMethodInvoker() {
		TestMethodAdapter adapter = new TestMethodAdapter();
		adapter.setTargetMethod("handle");
		adapter.setTargetObject(new PlainPojo());
		assertEquals("2.0.foo", adapter.getMessage(2, "foo"));
	}

	@Test
	void testEmptyParameters() {
		TestMethodAdapter adapter = new TestMethodAdapter();
		adapter.setTargetMethod("empty");
		adapter.setTargetObject(new PlainPojo());
		assertEquals(".", adapter.getMessage(2, "foo"));
	}

	@Test
	void testEmptyParametersEmptyArgs() {
		TestMethodAdapter adapter = new TestMethodAdapter();
		adapter.setTargetMethod("empty");
		adapter.setTargetObject(new PlainPojo());
		assertEquals(".", adapter.getMessage());
	}

	@Test
	void testMissingArgument() {
		TestMethodAdapter adapter = new TestMethodAdapter();
		adapter.setTargetMethod("missing");
		adapter.setTargetObject(new PlainPojo());
		assertEquals("foo.foo", adapter.getMessage(2, "foo"));
	}

	@Test
	void testWrongOrder() {
		TestMethodAdapter adapter = new TestMethodAdapter();
		adapter.setTargetMethod("disorder");
		adapter.setTargetObject(new PlainPojo());
		assertEquals("2.0.foo", adapter.getMessage(2, "foo"));
	}

	@Test
	void testTwoArgsOfSameTypeWithInexactMatch() throws Exception {
		HippyMethodInvoker invoker = new HippyMethodInvoker();
		invoker.setTargetMethod("duplicate");
		invoker.setTargetObject(new PlainPojo());
		invoker.setArguments("2", "foo");
		invoker.prepare();
		assertEquals("foo.2", invoker.invoke());
	}

	@Test
	void testOverloadedMethodUsingInputWithoutExactMatch() throws Exception {

		HippyMethodInvoker invoker = new HippyMethodInvoker();
		invoker.setTargetMethod("foo");
		@SuppressWarnings("unused")
		class OverloadingPojo {

			public Class<?> foo(List<?> arg) {
				return List.class;
			}

			public Class<?> foo(Set<?> arg) {
				return Set.class;
			}

		}

		TreeSet<Object> arg = new TreeSet<>();
		OverloadingPojo target = new OverloadingPojo();
		assertEquals(target.foo(arg), Set.class);

		invoker.setTargetObject(target);
		invoker.setArguments(arg);
		invoker.prepare();
		assertEquals(invoker.invoke(), Set.class);

	}

	@Test
	void testOverloadedMethodWithTwoArgumentsAndOneExactMatch() throws Exception {

		HippyMethodInvoker invoker = new HippyMethodInvoker();
		invoker.setTargetMethod("foo");
		@SuppressWarnings("unused")
		class OverloadingPojo {

			public Class<?> foo(String arg1, Number arg2) {
				return Number.class;
			}

			public Class<?> foo(String arg1, List<?> arg2) {
				return List.class;
			}

		}

		String exactArg = "string";
		Integer inexactArg = 0;
		OverloadingPojo target = new OverloadingPojo();
		assertEquals(target.foo(exactArg, inexactArg), Number.class);

		invoker.setTargetObject(target);
		invoker.setArguments(exactArg, inexactArg);
		invoker.prepare();
		assertEquals(invoker.invoke(), Number.class);

	}

	public static class PlainPojo {

		public String handle(double value, String input) {
			return value + "." + input;
		}

		public String disorder(String input, double value) {
			return value + "." + input;
		}

		public String duplicate(String input, Object value) {
			return value + "." + input;
		}

		public String missing(String input) {
			return input + "." + input;
		}

		public String empty() {
			return ".";
		}

	}

	public static interface Service {

		String getMessage(double value, String input);

	}

	public static class TestMethodAdapter extends AbstractMethodInvokingDelegator<String> implements Service {

		@Override
		public String getMessage(double value, String input) {
			try {
				return invokeDelegateMethodWithArguments(new Object[] { value, input });
			}
			catch (RuntimeException e) {
				throw e;
			}
			catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}

		public String getMessage() {
			try {
				return invokeDelegateMethodWithArguments(new Object[0]);
			}
			catch (RuntimeException e) {
				throw e;
			}
			catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}

	}

}
