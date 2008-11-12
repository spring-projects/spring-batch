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
package org.springframework.batch.core.configuration.util;

import static org.junit.Assert.*;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

/**
 * @author Lucas Ward
 *
 */
public class SimpleMethodInvokerTests {

	TestClass testClass;
	JobExecution jobExecution = new JobExecution(11L);
	
	@Before
	public void setUp(){
		testClass = new TestClass();
	}
	
	@Test
	public void testMethod() throws Exception{
		
		Method method = TestClass.class.getMethod("beforeJob");
		MethodInvoker methodInvoker = new SimpleMethodInvoker(testClass, method);
		methodInvoker.invokeMethod(jobExecution);
		assertTrue(testClass.beforeJobCalled);
	}
	
	@Test
	public void testMethodByName() throws Exception{
		
		MethodInvoker methodInvoker = new SimpleMethodInvoker(testClass, "beforeJob", JobExecutionListener.class);
		methodInvoker.invokeMethod(jobExecution);
		assertTrue(testClass.beforeJobCalled);
	}
	
	@Test
	public void testMethodWithExecution() throws Exception{
		Method method = TestClass.class.getMethod("beforeJobWithExecution", JobExecution.class);
		MethodInvoker methodInvoker = new SimpleMethodInvoker(testClass, method);
		methodInvoker.invokeMethod(jobExecution);
		assertTrue(testClass.beforeJobCalled);
	}
	
	@Test
	public void testMethodByNameWithExecution() throws Exception{
		MethodInvoker methodInvoker = new SimpleMethodInvoker(testClass, "beforeJobWithExecution", JobExecution.class);
		methodInvoker.invokeMethod(jobExecution);
		assertTrue(testClass.beforeJobCalled);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testMethodWithTooManyArguments() throws Exception{
		Method method = TestClass.class.getMethod("beforeJobWithTooManyArguments", JobExecution.class, int.class);
		MethodInvoker methodInvoker = new SimpleMethodInvoker(testClass, method);
		methodInvoker.invokeMethod(jobExecution);
		assertFalse(testClass.beforeJobCalled);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testMethodByNameWithTooManyArguments() throws Exception{
		MethodInvoker methodInvoker = new SimpleMethodInvoker(testClass, "beforeJobWithTooManyArguments", JobExecution.class);
		methodInvoker.invokeMethod(jobExecution);
		assertFalse(testClass.beforeJobCalled);
	}
	
	private class TestClass{
		
		boolean beforeJobCalled = false;
		
		public void beforeJob(){
			beforeJobCalled = true;
		}
		
		public void beforeJobWithExecution(JobExecution jobExecution){
			beforeJobCalled = true;
		}
		
		public void beforeJobWithTooManyArguments(JobExecution jobExecution, int someInt){
			beforeJobCalled = true;
		}
	}
}
