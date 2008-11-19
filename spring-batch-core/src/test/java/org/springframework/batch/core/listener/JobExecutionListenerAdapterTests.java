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
package org.springframework.batch.core.listener;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.annotation.AfterJob;
import org.springframework.batch.core.annotation.BeforeJob;

/**
 * @author Lucas Ward
 *
 */
public class JobExecutionListenerAdapterTests {

	private TestClass testClass;
	private AnnotatedTestClass annotatedTestClass;
	private InterfaceTestClass interfaceTestClass;
	private JobExecution jobExecution = new JobExecution(11L);
	
	
	@Before
	public void setUp(){
		testClass = new TestClass();
		annotatedTestClass = new AnnotatedTestClass();
		interfaceTestClass = new InterfaceTestClass();
	}
	
	@Test
	public void testBeforeJob() throws Exception{
		JobExecutionListenerAdapter adapter = new JobExecutionListenerAdapter(testClass);
		adapter.setBeforeMethod("beforeJob");
		adapter.afterPropertiesSet();
		adapter.beforeJob(jobExecution);
		adapter.afterJob(jobExecution);
		assertTrue(testClass.beforeJobCalled);
		assertFalse(testClass.afterJobCalled);
	}
	
	@Test
	public void testAfterJob() throws Exception{
		JobExecutionListenerAdapter adapter = new JobExecutionListenerAdapter(testClass);
		adapter.setAfterMethod("afterJob");
		adapter.afterPropertiesSet();
		adapter.beforeJob(jobExecution);
		adapter.afterJob(jobExecution);
		assertFalse(testClass.beforeJobCalled);
		assertTrue(testClass.afterJobCalled);
	}
	
	@Test
	public void testBoth() throws Exception{
		JobExecutionListenerAdapter adapter = new JobExecutionListenerAdapter(testClass);
		adapter.setAfterMethod("afterJob");
		adapter.setBeforeMethod("beforeJob");
		adapter.afterPropertiesSet();
		adapter.beforeJob(jobExecution);
		adapter.afterJob(jobExecution);
		assertTrue(testClass.beforeJobCalled);
		assertTrue(testClass.afterJobCalled);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNeither() throws Exception{
		JobExecutionListenerAdapter adapter = new JobExecutionListenerAdapter(testClass);
		adapter.afterPropertiesSet();
	}
	
	@Test
	public void testAnnotation() throws Exception{
		JobExecutionListenerAdapter adapter = new JobExecutionListenerAdapter(annotatedTestClass);
		adapter.afterPropertiesSet();
		adapter.beforeJob(jobExecution);
		adapter.afterJob(jobExecution);
		assertTrue(annotatedTestClass.beforeJobCalled);
		assertTrue(annotatedTestClass.afterJobCalled);
	}
	
	@Test
	public void testWithInterface() throws Exception{
		JobExecutionListenerAdapter adapter = new JobExecutionListenerAdapter(interfaceTestClass);
		adapter.afterPropertiesSet();
		adapter.beforeJob(jobExecution);
		adapter.afterJob(jobExecution);
		assertTrue(annotatedTestClass.beforeJobCalled);
		assertTrue(annotatedTestClass.afterJobCalled);
	}
	
	private class TestClass{
		
		boolean beforeJobCalled = false;
		boolean afterJobCalled = false;
		
		public void beforeJob(JobExecution jobExecution){
			beforeJobCalled = true;
		}
		
		public void afterJob(JobExecution jobExecution){
			afterJobCalled = true;
		}
	}
	
	private class AnnotatedTestClass extends TestClass{
		
		@BeforeJob
		public void before(){
			super.beforeJobCalled = true;
		}
		
		@AfterJob
		public void after(){
			super.afterJobCalled = true;
		}
	}
	
	private class InterfaceTestClass extends TestClass implements JobExecutionListener {
		
	}
	
}
