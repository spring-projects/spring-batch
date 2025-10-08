/*
 * Copyright 2009-2025 the original author or authors.
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

import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepLocator;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

/**
 * A {@link StepLocator} implementation that just looks in its enclosing bean factory for
 * {@link Step}s by name.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class BeanFactoryStepLocator implements StepLocator, BeanFactoryAware {

	private BeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/**
	 * Look up a bean with the provided name of type {@link Step}.
	 * @see StepLocator#getStep(String)
	 */
	@Override
	public Step getStep(String stepName) {
		return beanFactory.getBean(stepName, Step.class);
	}

}
