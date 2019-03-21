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
package org.springframework.batch.core.configuration.annotation;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Convenient factory for a {@link StepBuilder} which sets the {@link JobRepository} and
 * {@link PlatformTransactionManager} automatically.
 * 
 * @author Dave Syer
 * 
 */
public class StepBuilderFactory {

	private JobRepository jobRepository;

	private PlatformTransactionManager transactionManager;

	public StepBuilderFactory(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		this.jobRepository = jobRepository;
		this.transactionManager = transactionManager;
	}

	/**
	 * Creates a step builder and initializes its job repository and transaction manager. Note that if the builder is
	 * used to create a &#64;Bean definition then the name of the step and the bean name might be different.
	 * 
	 * @param name the name of the step
	 * @return a step builder
	 */
	public StepBuilder get(String name) {
		StepBuilder builder = new StepBuilder(name).repository(jobRepository).transactionManager(
				transactionManager);
		return builder;
	}

}
