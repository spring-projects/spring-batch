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

import org.springframework.batch.core.configuration.StepConfiguration;
import org.springframework.batch.core.executor.StepExecutor;
import org.springframework.batch.core.executor.StepExecutorFactory;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.execution.step.SimpleStepConfiguration;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.exception.handler.ExceptionHandler;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
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
	 * configuration is a {@link SimpleStepConfiguration} then a
	 * {@link StepExecutor} is created with policies matching the commit
	 * interval of the configuration. <br/>
	 * 
	 * @throws IllegalStateException
	 *             if the configuration is not a {@link SimpleStepConfiguration}.
	 * @throws IllegalStateException
	 *             if the {@link JobRepository} is null.
	 * 
	 * @see StepExecutorFactory#getExecutor(StepConfiguration)
	 */
	public StepExecutor getExecutor(StepConfiguration configuration) {

		Assert.notNull(jobRepository, "JobRepository cannot be null");
		Assert.state(configuration instanceof SimpleStepConfiguration,
				"StepConfiguration must be instance of SimpleStepConfiguration - found: ["
						+ (configuration == null ? null : configuration
								.getClass()) + "]");

		SimpleStepExecutor executor = new SimpleStepExecutor();
		executor.setRepository(jobRepository);
		RepeatTemplate template = new RepeatTemplate();
		RepeatOperations repeatOperations = template;
		SimpleStepConfiguration simpleConfiguration = (SimpleStepConfiguration) configuration;
		template.setCompletionPolicy(new SimpleCompletionPolicy(
				simpleConfiguration.getCommitInterval()));
		ExceptionHandler exceptionHandler = simpleConfiguration.getExceptionHandler();
		if (exceptionHandler!=null) {
			template.setExceptionHandler(exceptionHandler);
		}

		executor.setChunkOperations(repeatOperations);

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
