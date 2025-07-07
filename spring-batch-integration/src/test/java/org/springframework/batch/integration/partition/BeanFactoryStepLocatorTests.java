/*
 * Copyright 2009-2023 the original author or authors.
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
package org.springframework.batch.integration.partition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobInterruptedException;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class BeanFactoryStepLocatorTests {

	private final BeanFactoryStepLocator stepLocator = new BeanFactoryStepLocator();

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	@Test
	void testGetStep() {
		beanFactory.registerSingleton("foo", new StubStep("foo"));
		stepLocator.setBeanFactory(beanFactory);
		assertNotNull(stepLocator.getStep("foo"));
	}

	@Test
	void testGetStepNames() {
		beanFactory.registerSingleton("foo", new StubStep("foo"));
		beanFactory.registerSingleton("bar", new StubStep("bar"));
		stepLocator.setBeanFactory(beanFactory);
		assertEquals(2, stepLocator.getStepNames().size());
	}

	private static final class StubStep implements Step {

		private final String name;

		public StubStep(String name) {
			this.name = name;
		}

		@Override
		public void execute(StepExecution stepExecution) throws JobInterruptedException {
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public int getStartLimit() {
			return 0;
		}

	}

}
