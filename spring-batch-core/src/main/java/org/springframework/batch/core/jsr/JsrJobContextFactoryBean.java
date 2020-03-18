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

import javax.batch.runtime.StepExecution;
import javax.batch.runtime.context.JobContext;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.jsr.configuration.support.BatchPropertyContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

/**
 * Provides a single {@link JobContext} for each thread in a running job.
 * Subsequent calls to {@link FactoryBean#getObject()} on the same thread will
 * return the same instance.  The {@link JobContext} wraps a {@link JobExecution}
 * which is obtained in one of two ways:
 * <ul>
 *   <li>The current step scope (getting it from the current {@link StepExecution}</li>
 *   <li>The provided {@link JobExecution} via the {@link #setJobExecution(JobExecution)}
 * </ul>
 *
 * @author Michael Minella
 * @since 3.0
 */
public class JsrJobContextFactoryBean implements FactoryBean<JobContext> {

	private JobExecution jobExecution;
	@Autowired
	private BatchPropertyContext propertyContext;

	private static final ThreadLocal<JobContext> contextHolder = new ThreadLocal<>();

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 */
	@Override
	public JobContext getObject() throws Exception {
		return getCurrent();
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	@Override
	public Class<?> getObjectType() {
		return JobContext.class;
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.FactoryBean#isSingleton()
	 */
	@Override
	public boolean isSingleton() {
		return false;
	}

	/**
	 * Used to provide {@link JobContext} instances to batch artifacts that
	 * are not within the scope of a given step.
	 *
	 * @param jobExecution set the current {@link JobExecution}
	 */
	public void setJobExecution(JobExecution jobExecution) {
		Assert.notNull(jobExecution, "A JobExecution is required");
		this.jobExecution = jobExecution;
	}

	/**
	 * @param propertyContext the {@link BatchPropertyContext} to obtain job properties from
	 */
	public void setBatchPropertyContext(BatchPropertyContext propertyContext) {
		this.propertyContext = propertyContext;
	}

	/**
	 * Used to remove the {@link JobContext} for the current thread.  Not used via
	 * normal processing but useful for testing.
	 */
	public void close() {
		if(contextHolder.get() != null) {
			contextHolder.remove();
		}
	}

	private JobContext getCurrent() {
		if(contextHolder.get() == null) {
			JobExecution curJobExecution = null;

			if(StepSynchronizationManager.getContext() != null) {
				curJobExecution = StepSynchronizationManager.getContext().getStepExecution().getJobExecution();
			}

			if(curJobExecution != null) {
				jobExecution = curJobExecution;
			}

			if(jobExecution == null) {
				throw new FactoryBeanNotInitializedException("A JobExecution is required");
			}

			JsrJobContext jobContext = new JsrJobContext();
			jobContext.setJobExecution(jobExecution);

			if(propertyContext != null) {
				jobContext.setProperties(propertyContext.getJobProperties());
			} else {
				jobContext.setProperties(new Properties());
			}

			contextHolder.set(jobContext);
		}

		return contextHolder.get();
	}
}
