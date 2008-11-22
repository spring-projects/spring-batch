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
import org.springframework.batch.core.configuration.util.MethodInvoker;
import org.springframework.batch.core.configuration.util.MethodInvokerUtils;
import org.springframework.batch.core.configuration.util.SimpleMethodInvoker;

public class StepListenerMethodInterceptorTests {

	StepListenerMethodInterceptor interceptor;
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
		interceptor = new StepListenerMethodInterceptor(invokerMap);
		interceptor.invoke(new StubMethodInvocation(TestClass.class.getMethod("method1")));
		assertEquals(1, testClass.method1Count);
		interceptor.invoke(new StubMethodInvocation(TestClass.class.getMethod("method2")));
		assertEquals(1, testClass.method2Count);
	}
	
	@Test
	public void testMultipleInvokersPerName() throws Throwable{
		
		Map<String, Set<MethodInvoker>> invokerMap = new HashMap<String, Set<MethodInvoker>>();
		Set<MethodInvoker> invokers = asSet(MethodInvokerUtils.createMethodInvokerByName(testClass, "method1", false));
		invokers.add(MethodInvokerUtils.createMethodInvokerByName(testClass, "method2", false));
		invokerMap.put("method1", invokers);
		interceptor = new StepListenerMethodInterceptor(invokerMap);
		interceptor.invoke(new StubMethodInvocation(TestClass.class.getMethod("method1")));
		assertEquals(1, testClass.method1Count);
		interceptor.invoke(new StubMethodInvocation(TestClass.class.getMethod("method2")));
		assertEquals(1, testClass.method2Count);
	}
	
	@Test
	public void testExitStatusReturn() throws Throwable{
		Map<String, Set<MethodInvoker>> invokerMap = new HashMap<String, Set<MethodInvoker>>();
		Set<MethodInvoker> invokers = asSet(MethodInvokerUtils.createMethodInvokerByName(testClass, "method3", false));
		invokers.add(MethodInvokerUtils.createMethodInvokerByName(testClass, "method3", false));
		invokerMap.put("method3", invokers);
		interceptor = new StepListenerMethodInterceptor(invokerMap);
		assertEquals(ExitStatus.FINISHED, interceptor.invoke(new StubMethodInvocation(TestClass.class.getMethod("method3"))));
	}
	
	public Set<MethodInvoker> asSet(MethodInvoker methodInvoker){
		Set<MethodInvoker> invokerSet = new HashSet<MethodInvoker>();
		invokerSet.add(methodInvoker);
		return invokerSet;
	}
	
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
			return ExitStatus.FINISHED;
		}
	}
	
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
			// TODO Auto-generated method stub
			return null;
		}

		public AccessibleObject getStaticPart() {
			// TODO Auto-generated method stub
			return null;
		}

		public Object getThis() {
			// TODO Auto-generated method stub
			return null;
		}

		public Object proceed() throws Throwable {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
