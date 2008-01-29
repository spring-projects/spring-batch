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
import org.springframework.batch.core.domain.StepSupport;
import org.springframework.batch.core.executor.StepExecutor;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.tasklet.Tasklet;
import org.springframework.batch.repeat.exception.handler.ExceptionHandler;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

/**
 * A {@link Step} implementation that provides common behaviour to
 * subclasses.
 * 
 * @author Dave Syer
 * 
 */
public abstract class AbstractStep extends StepSupport {

	private int skipLimit = 0;

	private ExceptionHandler exceptionHandler;

	private JobRepository jobRepository;

	private PlatformTransactionManager transactionManager;

	private Tasklet tasklet;

	/**
	 * Default constructor.
	 */
	public AbstractStep() {
		super();
	}

	/**
	 * Convenient constructor for setting only the name property.
	 * @param name
	 */
	public AbstractStep(String name) {
		super(name);
	}

	public ExceptionHandler getExceptionHandler() {
		return exceptionHandler;
	}

	public void setExceptionHandler(ExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}

	public void setSkipLimit(int skipLimit) {
		this.skipLimit = skipLimit;
	}

	public int getSkipLimit() {
		return skipLimit;
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
	 * Public setter for the {@link PlatformTransactionManager}.
	 * @param transactionManager the transaction manager to set
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}
	
	/**
	 * Assert that all mandatory properties are set (the {@link JobRepository}).
	 * 
	 * @throws Exception
	 */
	public void afterPropertiesSet() throws Exception {
		assertMandatoryProperties();
	}

	protected void assertMandatoryProperties() {
		Assert.notNull(jobRepository, "JobRepository is mandatory");
		Assert.notNull(transactionManager, "TransactionManager is mandatory");
	}

	/**
	 * Create a {@link SimpleStepExecutor}.
	 * 
	 * @see org.springframework.batch.core.domain.Step#createStepExecutor()
	 */
	public StepExecutor createStepExecutor() {
		assertMandatoryProperties();
		SimpleStepExecutor executor = new SimpleStepExecutor();
		executor.setRepository(jobRepository);
		executor.applyConfiguration(this);
		executor.setTasklet(tasklet);
		executor.setTransactionManager(transactionManager);
		return executor;
	}

	/**
	 * Public setter for the tasklet.
	 * 
	 * @param tasklet the tasklet to set
	 */
	public void setTasklet(Tasklet tasklet) {
		this.tasklet = tasklet;
	}

}