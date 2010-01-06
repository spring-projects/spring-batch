package org.springframework.batch.item.adapter;

import static org.junit.Assert.assertEquals;

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
	
	public static class PlainPojo {
		public String handle(double value, String input) {
			return value+"."+input;
		}
		public String disorder(String input, double value) {
			return value+"."+input;
		}
		public String missing(String input) {
			return input+"."+input;
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
				return invokeDelegateMethodWithArguments(new Object[] {value, input});
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
