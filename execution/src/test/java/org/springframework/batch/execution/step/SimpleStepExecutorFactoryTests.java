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
package org.springframework.batch.execution.step;

import junit.framework.TestCase;

import org.springframework.batch.core.configuration.StepConfigurationSupport;
import org.springframework.batch.execution.step.simple.JobRepositorySupport;

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
			assertTrue(
					"Error message does not contain JobRepository: "
							+ e.getMessage(), e.getMessage().indexOf(
							"JobRepository") >= 0);
		}
	}
}
