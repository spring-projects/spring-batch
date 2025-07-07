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

import java.util.Arrays;
import java.util.Collection;

import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepLocator;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.util.Assert;

/**
 * A {@link StepLocator} implementation that just looks in its enclosing bean factory for
 * components of type {@link Step}.
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

	/**
	 * Look in the bean factory for all beans of type {@link Step}.
	 * @throws IllegalStateException if the {@link BeanFactory} is not listable
	 * @see StepLocator#getStepNames()
	 */
	@Override
	public Collection<String> getStepNames() {
		Assert.state(beanFactory instanceof ListableBeanFactory, "BeanFactory is not listable.");
		return Arrays.asList(((ListableBeanFactory) beanFactory).getBeanNamesForType(Step.class));
	}

}
