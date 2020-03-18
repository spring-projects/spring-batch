/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr;

import java.util.Properties;

import javax.batch.runtime.context.StepContext;

import org.springframework.batch.core.jsr.configuration.support.BatchPropertyContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

/**
 * {@link FactoryBean} implementation used to create {@link javax.batch.runtime.context.StepContext}
 * instances within the step scope.
 *
 * @author Michael Minella
 * @author Chris Schaefer
 * @since 3.0
 */
public class JsrStepContextFactoryBean implements FactoryBean<StepContext>, InitializingBean {
	@Autowired
	private BatchPropertyContext batchPropertyContext;

	private static final ThreadLocal<javax.batch.runtime.context.StepContext> contextHolder = new ThreadLocal<>();

	protected void setBatchPropertyContext(BatchPropertyContext batchPropertyContext) {
		this.batchPropertyContext = batchPropertyContext;
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 */
	@Override
	public StepContext getObject() throws Exception {
		return getCurrent();
	}

	private javax.batch.runtime.context.StepContext getCurrent() {
		org.springframework.batch.core.StepExecution curStepExecution = null;

		if(StepSynchronizationManager.getContext() != null) {
			curStepExecution = StepSynchronizationManager.getContext().getStepExecution();
		}

		if(curStepExecution == null) {
			throw new FactoryBeanNotInitializedException("A StepExecution is required");
		}

		StepContext context = contextHolder.get();

		// If the current context applies to the current step, use it
		if(context != null && context.getStepExecutionId() == curStepExecution.getId()) {
			return context;
		}

		Properties stepProperties = batchPropertyContext.getStepProperties(curStepExecution.getStepName());

		if(stepProperties != null) {
			context = new JsrStepContext(curStepExecution, stepProperties);
		} else {
			context = new JsrStepContext(curStepExecution, new Properties());
		}

		contextHolder.set(context);

		return context;
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

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(batchPropertyContext, "BatchPropertyContext is required");
	}

	public void remove() {
		if(contextHolder.get() != null) {
			contextHolder.remove();
		}
	}
}
