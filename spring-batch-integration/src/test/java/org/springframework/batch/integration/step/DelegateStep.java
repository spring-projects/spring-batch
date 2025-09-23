/*
 * Copyright 2006-2025 the original author or authors.
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
package org.springframework.batch.integration.step;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.util.Assert;

/**
 * Provides a wrapper for an existing {@link Step}, delegating execution to it, but
 * serving all other operations locally.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class DelegateStep extends AbstractStep {

	private Step delegate;

	/**
	 * Create a new instance of a {@link DelegateStep} with the given job repository.
	 * @param jobRepository the job repository to use. Must not be null.
	 * @since 6.0
	 */
	public DelegateStep(JobRepository jobRepository) {
		super(jobRepository);
	}

	/**
	 * @param delegate the delegate to set
	 */
	public void setDelegate(Step delegate) {
		this.delegate = delegate;
	}

	/**
	 * Check mandatory properties (delegate).
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(delegate != null, "A delegate Step must be provided");
		super.afterPropertiesSet();
	}

	@Override
	protected void doExecute(StepExecution stepExecution) throws Exception {
		delegate.execute(stepExecution);
	}

}