/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.execution.scope;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.execution.job.JobSupport;
import org.springframework.batch.execution.step.StepSupport;
import org.springframework.batch.repeat.synch.RepeatSynchronizationManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;

/**
 * @author Dave Syer
 *
 */
public class JobParametersAwareStepScopeTests extends TestCase {
	
	private StepScope scope = new StepScope();

	private SimpleStepContext context;

	JobParameters parameters = new JobParameters();

	/*
	 * (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		JobExecution jobExecution = new JobExecution(new JobInstance(new Long(1L), parameters, new JobSupport()), new Long(11L));
		context = new SimpleStepContext(jobExecution.createStepExecution(new StepSupport()));
		StepSynchronizationManager.register(context);
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		RepeatSynchronizationManager.clear();		
		super.tearDown();
	}

	public void testInjection() throws Exception {
		final TestBeanAware foo = new TestBeanAware();
		Object value = scope.get("foo", new ObjectFactory() {
			public Object getObject() throws BeansException {
				return foo;
			}
		});
		assertEquals(foo, value);
		assertTrue(context.hasAttribute("foo"));
		assertEquals(parameters, foo.getJobParameters());
	}

	public void testFailedInjection() throws Exception {
		// Null JobInstance so no parameters
		context.getStepExecution().getJobExecution().setJobInstance(null);
		final TestBeanAware foo = new TestBeanAware();
		Object value = scope.get("foo", new ObjectFactory() {
			public Object getObject() throws BeansException {
				return foo;
			}
		});
		assertEquals(foo, value);
		assertTrue(context.hasAttribute("foo"));
		assertEquals(null, foo.getJobParameters());
	}

	public static class TestBeanAware implements JobParametersAware {
		private JobParameters jobParameters;
		public void setJobParameters(JobParameters jobParameters) {
			this.jobParameters = jobParameters;
		}
		public JobParameters getJobParameters() {
			return jobParameters;
		}
	}
}
