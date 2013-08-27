/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr;

import java.util.Properties;

import org.springframework.batch.core.jsr.configuration.support.BatchPropertyContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * {@link FactoryBean} implementation used to create {@link javax.batch.runtime.context.StepContext}
 * instances within the step scope.
 *
 * @author Michael Minella
 * @author Chris Schaefer
 * @since 3.0
 */
public class StepContextFactoryBean implements FactoryBean<StepContext> {
	@Autowired
	private BatchPropertyContext batchPropertyContext;

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 */
	@Override
	public StepContext getObject() throws Exception {
		org.springframework.batch.core.scope.context.StepContext stepContext = StepSynchronizationManager.getContext();

		String stepName = stepContext.getStepName();
		String jobName = stepContext.getStepExecution().getJobExecution().getJobInstance().getJobName();
		Properties properties = batchPropertyContext.getStepLevelProperties(jobName + "." + stepName);

		return new StepContext(stepContext.getStepExecution(), properties);
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	@Override
	public Class<?> getObjectType() {
		return StepContext.class;
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.FactoryBean#isSingleton()
	 */
	@Override
	public boolean isSingleton() {
		return false;
	}
}
