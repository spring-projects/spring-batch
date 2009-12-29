package org.springframework.batch.core.listener;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.support.MethodInvoker;
import org.springframework.batch.support.MethodInvokerUtils;
import org.springframework.batch.support.SimpleMethodInvoker;

public class StepListenerMethodInterceptorTests {

	MethodInvokerMethodInterceptor interceptor;
	TestClass testClass;
	
	@Before
	public void setUp(){
		testClass = new TestClass();
	}
	
	@Test
	public void testNormalCase() throws Throwable{
		
		Map<String, Set<MethodInvoker>> invokerMap = new HashMap<String, Set<MethodInvoker>>();
		for(Method method : TestClass.class.getMethods()){
			invokerMap.put(method.getName(), asSet( new SimpleMethodInvoker(testClass, method)));
		}
		interceptor = new MethodInvokerMethodInterceptor(invokerMap);
		interceptor.invoke(new StubMethodInvocation(TestClass.class.getMethod("method1")));
		assertEquals(1, testClass.method1Count);
		interceptor.invoke(new StubMethodInvocation(TestClass.class.getMethod("method2")));
		assertEquals(1, testClass.method2Count);
	}
	
	@Test
	public void testMultipleInvokersPerName() throws Throwable{
		
		Map<String, Set<MethodInvoker>> invokerMap = new HashMap<String, Set<MethodInvoker>>();
		Set<MethodInvoker> invokers = asSet(MethodInvokerUtils.getMethodInvokerByName(testClass, "method1", false));
		invokers.add(MethodInvokerUtils.getMethodInvokerByName(testClass, "method2", false));
		invokerMap.put("method1", invokers);
		interceptor = new MethodInvokerMethodInterceptor(invokerMap);
		interceptor.invoke(new StubMethodInvocation(TestClass.class.getMethod("method1")));
		assertEquals(1, testClass.method1Count);
		interceptor.invoke(new StubMethodInvocation(TestClass.class.getMethod("method2")));
		assertEquals(1, testClass.method2Count);
	}
	
	@Test
	public void testExitStatusReturn() throws Throwable{
		Map<String, Set<MethodInvoker>> invokerMap = new HashMap<String, Set<MethodInvoker>>();
		Set<MethodInvoker> invokers = asSet(MethodInvokerUtils.getMethodInvokerByName(testClass, "method3", false));
		invokers.add(MethodInvokerUtils.getMethodInvokerByName(testClass, "method3", false));
		invokerMap.put("method3", invokers);
		interceptor = new MethodInvokerMethodInterceptor(invokerMap);
		assertEquals(ExitStatus.COMPLETED, interceptor.invoke(new StubMethodInvocation(TestClass.class.getMethod("method3"))));
	}
	
	public Set<MethodInvoker> asSet(MethodInvoker methodInvoker){
		Set<MethodInvoker> invokerSet = new HashSet<MethodInvoker>();
		invokerSet.add(methodInvoker);
		return invokerSet;
	}
	
	@SuppressWarnings("unused")
	private class TestClass{
		
		int method1Count = 0;
		int method2Count = 0;
		int method3Count = 0;
		
		public void method1(){
			method1Count++;
		}
		
		public void method2(){
			method2Count++;
		}
		
		public ExitStatus method3(){
			method3Count++;
			return ExitStatus.COMPLETED;
		}
	}
	
	@SuppressWarnings("unused")
	private class StubMethodInvocation implements MethodInvocation{

		Method method;
		Object[] args;
		
		public StubMethodInvocation(Method method, Object... args) {
			this.method = method;
			this.args = args;
		}
		
		public Method getMethod() {
			return method;
		}

		public Object[] getArguments() {
			return null;
		}

		public AccessibleObject getStaticPart() {
			return null;
		}

		public Object getThis() {
			return null;
		}

		public Object proceed() throws Throwable {
			return null;
		}
		
	}
}