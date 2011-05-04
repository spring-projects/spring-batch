package org.springframework.batch.item.adapter;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

public class HippyMethodInvokerTests {

	@Test
	public void testVanillaMethodInvoker() throws Exception {
		TestMethodAdapter adapter = new TestMethodAdapter();
		adapter.setTargetMethod("handle");
		adapter.setTargetObject(new PlainPojo());
		assertEquals("2.0.foo", adapter.getMessage(2, "foo"));
	}

	@Test
	public void testEmptyParameters() throws Exception {
		TestMethodAdapter adapter = new TestMethodAdapter();
		adapter.setTargetMethod("empty");
		adapter.setTargetObject(new PlainPojo());
		assertEquals(".", adapter.getMessage(2, "foo"));
	}

	@Test
	public void testEmptyParametersEmptyArgs() throws Exception {
		TestMethodAdapter adapter = new TestMethodAdapter();
		adapter.setTargetMethod("empty");
		adapter.setTargetObject(new PlainPojo());
		assertEquals(".", adapter.getMessage());
	}

	@Test
	public void testMissingArgument() throws Exception {
		TestMethodAdapter adapter = new TestMethodAdapter();
		adapter.setTargetMethod("missing");
		adapter.setTargetObject(new PlainPojo());
		assertEquals("foo.foo", adapter.getMessage(2, "foo"));
	}

	@Test
	public void testWrongOrder() throws Exception {
		TestMethodAdapter adapter = new TestMethodAdapter();
		adapter.setTargetMethod("disorder");
		adapter.setTargetObject(new PlainPojo());
		assertEquals("2.0.foo", adapter.getMessage(2, "foo"));
	}

	@Test
	public void testTwoArgsOfSameTypeWithInexactMatch() throws Exception {
		HippyMethodInvoker invoker = new HippyMethodInvoker();
		invoker.setTargetMethod("duplicate");
		invoker.setTargetObject(new PlainPojo());
		invoker.setArguments(new Object[] { "2", "foo" });
		invoker.prepare();
		assertEquals("foo.2", invoker.invoke());
	}

	@Test
	public void testOverloadedMethodUsingInputWithoutExactMatch() throws Exception {

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

		TreeSet<Object> arg = new TreeSet<Object>();
		OverloadingPojo target = new OverloadingPojo();
		assertEquals(target.foo(arg), Set.class);

		invoker.setTargetObject(target);
		invoker.setArguments(new Object[] { arg });
		invoker.prepare();
		assertEquals(invoker.invoke(), Set.class);

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
