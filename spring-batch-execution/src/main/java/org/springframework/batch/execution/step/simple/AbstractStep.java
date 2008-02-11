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
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.JobInterruptedException;
import org.springframework.batch.core.domain.StepSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemRecoverer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.stream.SimpleStreamManager;
import org.springframework.batch.item.stream.StreamManager;
import org.springframework.batch.repeat.exception.handler.ExceptionHandler;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

/**
 * A {@link Step} implementation that provides common behaviour to subclasses.
 * 
 * @author Dave Syer
 * @author Ben Hale
 */
public abstract class AbstractStep extends StepSupport implements InitializingBean {

	private int skipLimit = 0;

	private ExceptionHandler exceptionHandler;
	
	private RetryPolicy retryPolicy;

	private JobRepository jobRepository;

	private PlatformTransactionManager transactionManager;

	private StreamManager streamManager;

	private ItemReader itemReader;

	private ItemWriter itemWriter;

	private ItemRecoverer itemRecoverer;

	/**
	 * Default constructor.
	 */
	public AbstractStep() {
		super();
	}

	/**
	 * Convenient constructor for setting only the name property.
	 * 
	 * @param name
	 */
	public AbstractStep(String name) {
		super(name);
	}

	/**
	 * Public getter for the {@link RetryPolicy}.
	 * @return the {@link RetryPolicy}
	 */
	public RetryPolicy getRetryPolicy() {
		return retryPolicy;
	}

	/**
	 * Public setter for the {@link RetryPolicy}.
	 * @param retryPolicy the {@link RetryPolicy} to set
	 */
	public void setRetryPolicy(RetryPolicy retryPolicy) {
		this.retryPolicy = retryPolicy;
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
	 * Public setter for the {@link StreamManager}. Set either this or the transaction manager, but not both.
	 * 
	 * @param streamManager the {@link StreamManager} to set.
	 */
	public void setStreamManager(StreamManager streamManager) {
		this.streamManager = streamManager;
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

	/**
	 * @param itemRecoverer the itemRecoverer to set
	 */
	public void setItemRecoverer(ItemRecoverer itemRecoverer) {
		this.itemRecoverer = itemRecoverer;
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
		Assert.state(transactionManager != null || streamManager != null,
		        "Either StreamManager or TransactionManager must be set");
		Assert.state(transactionManager == null || streamManager == null,
		        "Only one of StreamManager or TransactionManager must be set");
		Assert.notNull(itemReader, "ItemReader must be provided");
		Assert.notNull(itemWriter, "ItemWriter must be provided");

	}

	public void execute(StepExecution stepExecution) throws JobInterruptedException, BatchCriticalException {
		SimpleStepExecutor executor = createStepExecutor();
		executor.execute(stepExecution);
	}

	/**
	 * @return a {@link SimpleStepExecutor} that can be used to launch the job.
	 * @throws BatchCriticalException
	 */
	protected SimpleStepExecutor createStepExecutor() throws BatchCriticalException {
		assertMandatoryProperties();
		// Do not set the streamManager field if it is null, otherwise
		// the mandatory properties check will fail.
		StreamManager manager = streamManager;
		if (streamManager == null) {
			manager = new SimpleStreamManager(transactionManager);
		}
		SimpleStepExecutor executor = new SimpleStepExecutor(this);
		executor.setItemReader(itemReader);
		executor.setItemWriter(itemWriter);
		executor.setItemRecoverer(itemRecoverer);
		executor.setRepository(jobRepository);
		executor.setRetryPolicy(retryPolicy);
		executor.setStreamManager(manager);
		try {
			executor.afterPropertiesSet();
		} catch (Exception e) {
			throw new BatchCriticalException(e);
		}
		executor.applyConfiguration(this);
		return executor;
	}
}