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

import org.springframework.batch.core.domain.Step;
import org.springframework.batch.core.executor.StepExecutor;
import org.springframework.batch.core.executor.StepExecutorFactory;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * A {@link StepExecutorFactory} that only knows how to create
 * {@link SimpleStepExecutor} instances.
 * 
 * @author Dave Syer
 * 
 */
public class SimpleStepExecutorFactory implements StepExecutorFactory,
		InitializingBean {

	private JobRepository jobRepository;

	/**
	 * Create a {@link SimpleStepExecutor} for this configuration. If the
	 * configuration is a {@link SimpleStep} then a
	 * {@link StepExecutor} is created with policies matching the commit
	 * interval of the configuration. <br/>
	 * 
	 * @throws IllegalStateException
	 *             if the configuration is not a {@link SimpleStep}.
	 * @throws IllegalStateException
	 *             if the {@link JobRepository} is null.
	 * 
	 * @see StepExecutorFactory#getExecutor(Step)
	 */
	public StepExecutor getExecutor(Step step) {

		Assert.notNull(jobRepository, "JobRepository cannot be null");
		Assert.state(step instanceof AbstractStep,
				"Step must be instance of SimpleStep - found: ["
						+ (step == null ? null : step
								.getClass()) + "]");

		SimpleStepExecutor executor = new SimpleStepExecutor();
		executor.setRepository(jobRepository);
		executor.applyConfiguration(step);

		return executor;

	}

	/**
	 * Public setter for {@link JobRepository}.
	 * 
	 * @param jobRepository
	 *            is a mandatory dependence (no default).
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * Assert that all mandatory properties are set (the {@link JobRepository}).
	 * 
	 * @throws Exception
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(jobRepository);
	}
}
