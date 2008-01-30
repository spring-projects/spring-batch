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
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.exception.handler.ExceptionHandler;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;

/**
 * @author Dave Syer
 * 
 */
public class SimpleStepTests extends TestCase {

	public void testSuccessfulStepExecutor() throws Exception {
		SimpleStep step = new SimpleStep();
		step.setJobRepository(new JobRepositorySupport());
		step.setTransactionManager(new ResourcelessTransactionManager());
		assertNotNull(step.createStepExecutor());
	}

	public void testSuccessfulExceptionHandler() throws Exception {
		SimpleStep configuration = new SimpleStep("foo");
		configuration.setJobRepository(new JobRepositorySupport());
		configuration.setTransactionManager(new ResourcelessTransactionManager());
		final List list = new ArrayList();
		configuration.setExceptionHandler(new ExceptionHandler() {
			public void handleException(RepeatContext context,
					Throwable throwable) throws RuntimeException {
				list.add(throwable);
				throw new RuntimeException("Oops");
			}
		});
		SimpleStepExecutor executor = (SimpleStepExecutor) configuration
				.createStepExecutor();
		StepExecution stepExecution = new StepExecution(new StepInstance(
				new Long(11)), new JobExecution(new JobInstance(new Long(0L), new JobParameters()),
				new Long(12)));
		try {
			executor.execute(stepExecution);
			fail("Expected RuntimeException");
		} catch (NullPointerException e) {
			throw e;
		}catch (RuntimeException e) {
			assertEquals("Oops", e.getMessage());
		}
		assertEquals(1, list.size());
	}

	public void testUnsuccessfulNoJobRepository() throws Exception {
		try {
			new SimpleStep().createStepExecutor();
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// expected
			assertTrue("Error message does not contain JobRepository: "
					+ e.getMessage(),
					e.getMessage().indexOf("JobRepository") >= 0);
		}
	}

	public void testMandatoryProperties() throws Exception {
		try {
			new SimpleStep().afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

}
