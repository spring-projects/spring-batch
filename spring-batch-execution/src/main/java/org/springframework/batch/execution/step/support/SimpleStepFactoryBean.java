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
package org.springframework.batch.execution.step.support;

import org.springframework.batch.core.domain.ItemSkipPolicy;
import org.springframework.batch.core.domain.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.execution.step.ItemOrientedStep;
import org.springframework.batch.item.ItemKeyGenerator;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.exception.handler.ExceptionHandler;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.callback.ItemReaderRetryCallback;
import org.springframework.batch.retry.policy.ItemReaderRetryPolicy;
import org.springframework.batch.retry.support.RetryTemplate;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

/**
 * @author Dave Syer
 * 
 */
public class SimpleStepFactoryBean extends AbstractFactoryBean implements BeanNameAware {

	private String name;

	private int commitInterval = 0;

	private int startLimit = Integer.MAX_VALUE;

	private boolean allowStartIfComplete;

	private ItemReader itemReader;

	private ItemWriter itemWriter;
	
	private Object[] listeners = new Object[0];

	private PlatformTransactionManager transactionManager;

	private JobRepository jobRepository;

	private ItemSkipPolicy itemSkipPolicy = new NeverSkipItemSkipPolicy();

	private RepeatOperations stepOperations = new RepeatTemplate();

	private RetryPolicy retryPolicy = null;

	private ExceptionHandler exceptionHandler;

	private ItemKeyGenerator itemKeyGenerator;

	private ItemKeyGenerator defaultKeyGenerator = new ItemKeyGenerator() {
		public Object getKey(Object item) {
			return item;
		}
	};

	/**
	 * Set the bean name property, which will become the name of the
	 * {@link Step} when it is created.
	 * 
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	public void setBeanName(String name) {
		this.name = name;
	}

	/**
	 * Set the commit interval.
	 * 
	 * @param commitInterval
	 */
	public void setCommitInterval(int commitInterval) {
		this.commitInterval = commitInterval;
	}

	/**
	 * Public setter for the startLimit.
	 * 
	 * @param startLimit the startLimit to set
	 */
	public void setStartLimit(int startLimit) {
		this.startLimit = startLimit;
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
	 * @param listeners
	 */
	public void setListeners(Object[] listeners) {
		this.listeners = listeners;
	}

	/**
	 * @param itemSkipPolicy
	 */
	public void setItemSkipPolicy(ItemSkipPolicy itemSkipPolicy) {
		this.itemSkipPolicy = itemSkipPolicy;
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
	 * Public setter for the RepeatOperations.
	 * @param stepOperations the stepOperations to set
	 */
	public void setStepOperations(RepeatOperations stepOperations) {
		this.stepOperations = stepOperations;
	}

	/**
	 * Public setter for the {@link RetryPolicy}.
	 * @param retryPolicy the {@link RetryPolicy} to set
	 */
	public void setRetryPolicy(RetryPolicy retryPolicy) {
		this.retryPolicy = retryPolicy;
	}

	/**
	 * Set the {@link ExceptionHandler} for the step operations (outer loop).
	 * @param exceptionHandler
	 */
	public void setExceptionHandler(ExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * Public setter for the {@link ItemKeyGenerator}. If it is not injected
	 * but the reader or writer implement {@link ItemKeyGenerator}, one of
	 * those will be used instead (preferring the reader to the writer if both
	 * would be appropriate).
	 * 
	 * @param itemKeyGenerator the {@link ItemKeyGenerator} to set
	 */
	public void setItemKeyGenerator(ItemKeyGenerator itemKeyGenerator) {
		this.itemKeyGenerator = itemKeyGenerator;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.AbstractFactoryBean#createInstance()
	 */
	protected Object createInstance() throws Exception {
		ItemOrientedStep step = new ItemOrientedStep(name);
		applyConfiguration(step);
		return step;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.AbstractFactoryBean#getObjectType()
	 */
	public Class getObjectType() {
		return Step.class;
	}

	/**
	 * @param step
	 * 
	 */
	private void applyConfiguration(ItemOrientedStep step) {

		Assert.notNull(itemReader, "ItemReader must be provided");
		Assert.notNull(itemWriter, "ItemWriter must be provided");
		Assert.notNull(jobRepository, "JobRepository must be provided");
		Assert.notNull(transactionManager, "TransactionManager must be provided");

		step.setItemReader(itemReader);
		step.setItemWriter(itemWriter);
		step.setTransactionManager(transactionManager);
		step.setJobRepository(jobRepository);
		step.setStartLimit(startLimit);
		step.setListeners(listeners);
		step.setItemSkipPolicy(itemSkipPolicy);
		step.setAllowStartIfComplete(allowStartIfComplete);

		itemKeyGenerator = getKeyGenerator();

		if (retryPolicy != null) {
			ItemReaderRetryCallback retryCallback = new ItemReaderRetryCallback(itemReader, itemKeyGenerator,
					itemWriter);
			ItemReaderRetryPolicy itemProviderRetryPolicy = new ItemReaderRetryPolicy(retryPolicy);
			RetryTemplate template = new RetryTemplate();
			template.setRetryPolicy(itemProviderRetryPolicy);
			step.setRetryOperations(template);
			step.setRetryCallback(retryCallback);
		}

		if (commitInterval > 0) {
			RepeatTemplate chunkOperations = new RepeatTemplate();
			chunkOperations.setCompletionPolicy(new SimpleCompletionPolicy(commitInterval));
			step.setChunkOperations(chunkOperations);
		}

		if (exceptionHandler != null && stepOperations instanceof RepeatTemplate) {
			((RepeatTemplate) stepOperations).setExceptionHandler(exceptionHandler);
			step.setStepOperations(stepOperations);
		}
	}

	/**
	 * @return
	 */
	private ItemKeyGenerator getKeyGenerator() {
		if (itemKeyGenerator != null) {
			return itemKeyGenerator;
		}
		if (itemReader instanceof ItemKeyGenerator) {
			return (ItemKeyGenerator) itemReader;
		}
		if (itemWriter instanceof ItemKeyGenerator) {
			return (ItemKeyGenerator) itemWriter;
		}
		return defaultKeyGenerator;

	}

}
