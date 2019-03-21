/*
 * Copyright 2012-2013 the original author or authors.
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

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.beans.factory.FactoryBean;

/**
 * Convenience factory for {@link Step} instances given a {@link StepLocator}.
 * Most implementations of {@link Job} implement StepLocator, so that can be a
 * good starting point.
 *
 * @author Dave Syer
 *
 */
public class StepLocatorStepFactoryBean implements FactoryBean<Step> {

	public StepLocator stepLocator;

	public String stepName;

	/**
	 * @param stepLocator instance of {@link StepLocator} to be used by the factory bean.
	 */
	public void setStepLocator(StepLocator stepLocator) {
		this.stepLocator = stepLocator;
	}

	/**
	 * @param stepName the name to be associated with the step.
	 */
	public void setStepName(String stepName) {
		this.stepName = stepName;
	}

	/**
	 *
	 * @see FactoryBean#getObject()
	 */
	@Override
	public Step getObject() throws Exception {
		return stepLocator.getStep(stepName);
	}

	/**
	 * Tell clients that we are a factory for {@link Step} instances.
	 *
	 * @see FactoryBean#getObjectType()
	 */
	@Override
	public Class<? extends Step> getObjectType() {
		return Step.class;
	}

	/**
	 * Always return true as optimization for bean factory.
	 *
	 * @see FactoryBean#isSingleton()
	 */
	@Override
	public boolean isSingleton() {
		return true;
	}

}
