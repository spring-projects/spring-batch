package org.springframework.batch.execution.bootstrap.support;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeMismatchException;

public class TypeConverterMethodInterceptorTests extends TestCase {

	TypeConverterMethodInterceptor interceptor = new TypeConverterMethodInterceptor();

	private List list = new ArrayList();

	/**
	 * Even though TestBean does not implement Test, the proxy will invoke a
	 * method is called with the same signature.
	 * 
	 * @throws Exception
	 */
	public void testInvokeWithNull() {
		ProxyFactory factory = new ProxyFactory(Test.class, interceptor);
		factory.setTarget(new TestBean(true));
		Test proxy = (Test) factory.getProxy();
		assertEquals(0, list.size());
		proxy.operate();
		assertEquals(1, list.size());
	}

	/**
	 * Even though TestBean does not implement Test, the proxy will return
	 * something if a method is called with the same signature.
	 * 
	 * @throws Exception
	 */
	public void testInvokeWithBoolean() {
		ProxyFactory factory = new ProxyFactory(Test.class, interceptor);
		factory.setTarget(new TestBean(true));
		Test proxy = (Test) factory.getProxy();
		assertEquals(true, proxy.isTest());
	}

	/**
	 * Even though TestBean does not implement Test, the proxy will return
	 * something if a method is called with the same signature.
	 * 
	 * @throws Exception
	 */
	public void testInvokeWithConversionToInt() {
		ProxyFactory factory = new ProxyFactory(Test.class, interceptor);
		factory.setTarget(new TestBean(true));
		Test proxy = (Test) factory.getProxy();
		assertEquals(123, proxy.getValue());
	}

	/**
	 * Even though TestBean does not implement Test, the proxy will return a
	 * converted value if a method is called with the same name and arguments,
	 * but different return type.
	 * 
	 * @throws Exception
	 */
	public void testInvokeWithComplex() throws Exception {
		ProxyFactory factory = new ProxyFactory(Test.class, interceptor);
		factory.setTarget(new TestBean(true));
		Test proxy = (Test) factory.getProxy();
		assertEquals("FOO:true", proxy.getBean());
	}

	public void testInvalidConversion() throws Exception {
		ProxyFactory factory = new ProxyFactory(Test.class, interceptor);
		factory.setTarget(new TestBean(true));
		Test proxy = (Test) factory.getProxy();
		try {
			proxy.getInvalid();
			fail("Expected TypeMismatchException");
		} catch (TypeMismatchException e) {
			// expected
		}
	}
	
	public void testTypeConverter() throws Exception {
		final TestCase testCase = this;
		interceptor.setTypeConverter(new SimpleTypeConverter() {
			public Object convertIfNecessary(Object value, Class requiredType) {
				return testCase;
			}
		});
		ProxyFactory factory = new ProxyFactory(Test.class, interceptor);
		factory.setTarget(new TestBean(true));
		Test proxy = (Test) factory.getProxy();
		assertEquals(testCase, proxy.getInvalid());
	}

	public interface Test {
		boolean isTest();

		String getBean();

		TestCase getInvalid();
		
		int getValue();

		void operate();
	}

	// N.B. TestBean intentionally  does not implement Test!
	public class TestBean {
		private boolean test;

		public TestBean(boolean test) {
			super();
			this.test = test;
		}

		public boolean isTest() {
			return test;
		}

		public TestBean getBean() {
			return this;
		}

		public TestBean getInvalid() {
			return this;
		}
		
		public String getValue() {
			return "123";
		}

		public void operate() {
			list.add("FOO");
		}

		public String toString() {
			return "FOO:" + test;
		}
	}

}
