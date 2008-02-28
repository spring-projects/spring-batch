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
package org.springframework.batch.execution.step;

import org.springframework.batch.core.domain.ItemFailureHandler;
import org.springframework.batch.core.domain.ItemSkipPolicy;
import org.springframework.batch.core.domain.JobInterruptedException;
import org.springframework.batch.core.domain.Step;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.execution.step.support.DefaultItemFailureHandler;
import org.springframework.batch.execution.step.support.NeverSkipItemSkipPolicy;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.exception.handler.ExceptionHandler;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

/**
 * A {@link Step} implementation that provides common behaviour to subclasses.
 * 
 * @author Dave Syer
 * @author Ben Hale
 */
public abstract class AbstractStep implements Step, InitializingBean, BeanNameAware {

	protected ExceptionHandler exceptionHandler;

	protected RetryPolicy retryPolicy;

	protected JobRepository jobRepository;

	protected PlatformTransactionManager transactionManager;

	protected ItemReader itemReader;

	protected ItemWriter itemWriter;

	protected ItemSkipPolicy itemSkipPolicy = new NeverSkipItemSkipPolicy();

	protected ItemFailureHandler itemFailureHandler = new DefaultItemFailureHandler();

	protected String name;

	protected int startLimit = Integer.MAX_VALUE;

	protected boolean allowStartIfComplete;

	/**
	 * Default constructor.
	 */
	public AbstractStep() {
		super();
	}

	public String getName() {
		return this.name;
	}

	/**
	 * Set the name property if it is not already set. Because of the order of
	 * the callbacks in a Spring container the name property will be set first
	 * if it is present. Care is needed with bean definition inheritance - if a
	 * parent bean has a name, then its children need an explicit name as well,
	 * otherwise they will not be unique.
	 * 
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	public void setBeanName(String name) {
		if (this.name == null) {
			this.name = name;
		}
	}

	/**
	 * Set the name property. Always overrides the default value if this object
	 * is a Spring bean.
	 * 
	 * @see #setBeanName(java.lang.String)
	 */
	public void setName(String name) {
		this.name = name;
	}

	public int getStartLimit() {
		return this.startLimit;
	}

	/**
	 * Public setter for the startLimit.
	 * 
	 * @param startLimit the startLimit to set
	 */
	public void setStartLimit(int startLimit) {
		this.startLimit = startLimit;
	}

	public boolean isAllowStartIfComplete() {
		return this.allowStartIfComplete;
	}

	/**
	 * Public setter for the shouldAllowStartIfComplete.
	 * 
	 * @param allowStartIfComplete the shouldAllowStartIfComplete to set
	 */
	public void setAllowStartIfComplete(boolean allowStartIfComplete) {
		this.allowStartIfComplete = allowStartIfComplete;
	}

	/**
	 * Convenient constructor for setting only the name property.
	 * 
	 * @param name
	 */
	public AbstractStep(String name) {
		this.name = name;
	}

	/**
	 * Public setter for the {@link RetryPolicy}.
	 * @param retryPolicy the {@link RetryPolicy} to set
	 */
	public void setRetryPolicy(RetryPolicy retryPolicy) {
		this.retryPolicy = retryPolicy;
	}

	public void setExceptionHandler(ExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * Public setter for {@link JobRepository}.
	 * 
	 * @param jobRepository is a mandatory dependence (no default).
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * Public setter for the {@link PlatformTransactionManager}.
	 * 
	 * @param transactionManager the transaction manager to set
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * @param itemReader the itemReader to set
	 */
	public void setItemReader(ItemReader itemReader) {
		this.itemReader = itemReader;
	}

	/**
	 * @param itemWriter the itemWriter to set
	 */
	public void setItemWriter(ItemWriter itemWriter) {
		this.itemWriter = itemWriter;
	}

	public void setItemSkipPolicy(ItemSkipPolicy itemSkipPolicy) {
		this.itemSkipPolicy = itemSkipPolicy;
	}

	public void setItemFailureHandler(ItemFailureHandler itemFailureHandler) {
		this.itemFailureHandler = itemFailureHandler;
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
		Assert.notNull(transactionManager, "TransactionManager must be set");
		Assert.notNull(itemReader, "ItemReader must be provided");
		Assert.notNull(itemWriter, "ItemWriter must be provided");

	}

	public abstract void execute(StepExecution stepExecution) throws JobInterruptedException, BatchCriticalException;
}