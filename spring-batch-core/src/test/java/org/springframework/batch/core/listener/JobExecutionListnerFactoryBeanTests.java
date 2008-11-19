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

import static org.junit.Assert.*;

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
public class JobExecutionListnerFactoryBeanTests {

	JobExecutionListenerFactoryBean factoryBean;
	
	@Before
	public void setUp(){
		factoryBean = new JobExecutionListenerFactoryBean();
	}
	
	@Test
	public void testWithInterface() throws Exception{
		JobListenerWithInterface delegate = new JobListenerWithInterface();
		factoryBean.setDelegate(delegate);
		assertEquals(delegate,factoryBean.getObject());
	}
	
	@Test
	public void testWithAnnotations() throws Exception{
		AnnotatedTestClass delegate = new AnnotatedTestClass();
		factoryBean.setDelegate(delegate);
		JobExecutionListener listener = (JobExecutionListener) factoryBean.getObject();
		JobExecution jobExecution = new JobExecution(11L);
		listener.beforeJob(jobExecution);
		listener.afterJob(jobExecution);
		assertTrue(delegate.beforeJobCalled);
		assertTrue(delegate.afterJobCalled);
	}
	
	private class JobListenerWithInterface implements JobExecutionListener{

		public void afterJob(JobExecution jobExecution) {
		}

		public void beforeJob(JobExecution jobExecution) {
		}
		
	}
	
	private class AnnotatedTestClass {
		
		boolean beforeJobCalled = false;
		boolean afterJobCalled = false;
		
		@BeforeJob
		public void before(){
			beforeJobCalled = true;
		}
		
		@AfterJob
		public void after(){
			afterJobCalled = true;
		}
	}
}
