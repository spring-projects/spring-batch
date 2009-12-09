/*
 * Copyright 2002-2008 the original author or authors.
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

/*
 * Copyright 2002-2008 the original author or authors.
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
package org.springframework.batch.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.springframework.util.Assert;

/**
 * @author Lucas Ward
 *
 */
public class SimpleMethodInvokerTests {

	TestClass testClass;
	String value = "foo";
	@Before
	public void setUp(){
		testClass = new TestClass();
	}
	
	@Test
	public void testMethod() throws Exception{
		
		Method method = TestClass.class.getMethod("before");
		MethodInvoker methodInvoker = new SimpleMethodInvoker(testClass, method);
		methodInvoker.invokeMethod(value);
		assertTrue(testClass.beforeCalled);
	}
	
	@Test
	public void testMethodByName() throws Exception{
		
		MethodInvoker methodInvoker = new SimpleMethodInvoker(testClass, "before", String.class);
		methodInvoker.invokeMethod(value);
		assertTrue(testClass.beforeCalled);
	}
	
	@Test
	public void testMethodWithExecution() throws Exception{
		Method method = TestClass.class.getMethod("beforeWithArgument", String.class);
		MethodInvoker methodInvoker = new SimpleMethodInvoker(testClass, method);
		methodInvoker.invokeMethod(value);
		assertTrue(testClass.beforeCalled);
	}
	
	@Test
	public void testMethodByNameWithExecution() throws Exception{
		MethodInvoker methodInvoker = new SimpleMethodInvoker(testClass, "beforeWithArgument", String.class);
		methodInvoker.invokeMethod(value);
		assertTrue(testClass.beforeCalled);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testMethodWithTooManyArguments() throws Exception{
		Method method = TestClass.class.getMethod("beforeWithTooManyArguments", String.class, int.class);
		MethodInvoker methodInvoker = new SimpleMethodInvoker(testClass, method);
		methodInvoker.invokeMethod(value);
		assertFalse(testClass.beforeCalled);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testMethodByNameWithTooManyArguments() throws Exception{
		MethodInvoker methodInvoker = new SimpleMethodInvoker(testClass, "beforeWithTooManyArguments", String.class);
		methodInvoker.invokeMethod(value);
		assertFalse(testClass.beforeCalled);
	}
	
	@Test
	public void testMethodWithArgument() throws Exception{
		MethodInvoker methodInvoker = new SimpleMethodInvoker(testClass, "argumentTest", Object.class);
		methodInvoker.invokeMethod(new Object());
		assertTrue(testClass.argumentTestCalled);
	}
	
	@Test
	public void testEquals() throws Exception{
		Method method = TestClass.class.getMethod("beforeWithArgument", String.class);
		MethodInvoker methodInvoker = new SimpleMethodInvoker(testClass, method);
		
		method = TestClass.class.getMethod("beforeWithArgument", String.class);
		MethodInvoker methodInvoker2 = new SimpleMethodInvoker(testClass, method);
		assertEquals(methodInvoker, methodInvoker2);
	}
	
	@SuppressWarnings("unused")
	private class TestClass{
		
		boolean beforeCalled = false;
		boolean argumentTestCalled = false;
		
		public void before(){
			beforeCalled = true;
		}
		
		public void beforeWithArgument(String value){
			beforeCalled = true;
		}
		
		public void beforeWithTooManyArguments(String value, int someInt){
			beforeCalled = true;
		}
		
		public void argumentTest(Object object){
			Assert.notNull(object);
			argumentTestCalled = true;
		}
	}
}
