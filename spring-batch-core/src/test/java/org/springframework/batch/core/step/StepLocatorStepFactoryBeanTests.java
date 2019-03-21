/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.step;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.SimpleJob;

/**
 * Tests for StepLocatorStepFactoryBean.
 *
 * @author tvaughan
 */
public class StepLocatorStepFactoryBeanTests {

	@Test
	public void testFoo() throws Exception {
		Step testStep1 = buildTestStep("foo");
		Step testStep2 = buildTestStep("bar");
		Step testStep3 = buildTestStep("baz");

		SimpleJob simpleJob = new SimpleJob();   // is a StepLocator
		simpleJob.addStep(testStep1);
		simpleJob.addStep(testStep2);
		simpleJob.addStep(testStep3);

		StepLocatorStepFactoryBean stepLocatorStepFactoryBean = new StepLocatorStepFactoryBean();
		stepLocatorStepFactoryBean.setStepLocator(simpleJob);
		stepLocatorStepFactoryBean.setStepName("bar");
		assertEquals(testStep2, stepLocatorStepFactoryBean.getObject());
	}

	private Step buildTestStep(final String stepName) {
		return new Step() {
			@Override
			public String getName() {
				return stepName;
			}

			@Override
			public boolean isAllowStartIfComplete() {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public int getStartLimit() {
				throw new UnsupportedOperationException("Not supported yet.");
			}

			@Override
			public void execute(StepExecution stepExecution) throws JobInterruptedException {
				throw new UnsupportedOperationException("Not supported yet.");
			}
		};
	}

	@Test
	public void testGetObjectType() {
		assertTrue((new StepLocatorStepFactoryBean()).getObjectType().isAssignableFrom(Step.class));
	}
}
