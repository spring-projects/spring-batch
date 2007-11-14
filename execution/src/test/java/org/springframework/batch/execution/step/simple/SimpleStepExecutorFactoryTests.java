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
package org.springframework.batch.execution.step.simple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.configuration.StepConfigurationSupport;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.execution.step.RepeatOperationsHolder;
import org.springframework.batch.execution.step.SimpleStepConfiguration;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.exception.handler.ExceptionHandler;
import org.springframework.batch.repeat.interceptor.RepeatInterceptorAdapter;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;

/**
 * @author Dave Syer
 * 
 */
public class SimpleStepExecutorFactoryTests extends TestCase {

	private SimpleStepExecutorFactory factory = new SimpleStepExecutorFactory();

	protected void setUp() throws Exception {
		factory.setJobRepository(new JobRepositorySupport());
	}

	public void testSuccessfulStepExecutor() throws Exception {
		assertNotNull(factory.getExecutor(new SimpleStepConfiguration()));
	}

	public void testSuccessfulExceptionHandler() throws Exception {
		SimpleStepConfiguration configuration = new SimpleStepConfiguration();
		final List list = new ArrayList();
		configuration.setExceptionHandler(new ExceptionHandler() {
			public void handleExceptions(RepeatContext context,
					Collection throwables) throws RuntimeException {
				list.addAll(throwables);
				throw new RuntimeException("Oops");
			}
		});
		SimpleStepExecutor executor = (SimpleStepExecutor) factory
				.getExecutor(configuration);
		StepExecution stepExecution = new StepExecution(new StepInstance(
				new Long(11)), new JobExecution(new JobInstance(null),
				new Long(12)));
		try {
			executor.processChunk(configuration, stepExecution);
			fail("Expected RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Oops", e.getMessage());
		}
		assertEquals(1, list.size());
	}

	public void testSuccessfulRepeatOperationsHolder() throws Exception {
		RepeatTemplate repeatTemplate = new RepeatTemplate();
		final List list = new ArrayList();
		repeatTemplate.setInterceptor(new RepeatInterceptorAdapter() {
			public void onError(RepeatContext context, Throwable e) {
				list.add(e);
			}
		});
		repeatTemplate.setCompletionPolicy(new SimpleCompletionPolicy(2));
		SimpleHolderStepConfiguration configuration = new SimpleHolderStepConfiguration(
				repeatTemplate);
		SimpleStepExecutor executor = (SimpleStepExecutor) factory
				.getExecutor(configuration);
		StepExecution stepExecution = new StepExecution(new StepInstance(
				new Long(11)), new JobExecution(new JobInstance(null),
				new Long(12)));
		try {
			executor.processChunk(configuration, stepExecution);
			fail("Expected RuntimeException");
		} catch (NullPointerException e) {
			// expected
		}
		assertEquals(1, list.size());
	}

	public void testUnsuccessfulWrongConfiguration() throws Exception {
		try {
			factory.getExecutor(new StepConfigurationSupport());
			fail("Expected IllegalStateException");
		} catch (IllegalStateException e) {
			// expected
			assertTrue(
					"Error message does not contain SimpleStepConfiguration: "
							+ e.getMessage(), e.getMessage().indexOf(
							"SimpleStepConfiguration") >= 0);
		}
	}

	public void testUnsuccessfulNoJobRepository() throws Exception {
		try {
			factory = new SimpleStepExecutorFactory();
			factory.getExecutor(new SimpleStepConfiguration());
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// expected
			assertTrue("Error message does not contain JobRepository: "
					+ e.getMessage(),
					e.getMessage().indexOf("JobRepository") >= 0);
		}
	}

	public void testMandatoryProperties() throws Exception {
		factory = new SimpleStepExecutorFactory();
		try {
			factory.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	/**
	 * @author Dave Syer
	 * 
	 */
	public class SimpleHolderStepConfiguration extends SimpleStepConfiguration
			implements RepeatOperationsHolder {
		private RepeatOperations executor;

		public SimpleHolderStepConfiguration(RepeatOperations executor) {
			this.executor = executor;
		}

		public RepeatOperations getChunkOperations() {
			return executor;
		}
	}

}
