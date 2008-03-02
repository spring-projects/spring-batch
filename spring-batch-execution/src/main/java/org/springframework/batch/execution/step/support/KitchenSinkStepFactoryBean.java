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
import org.springframework.batch.execution.step.ItemOrientedStep;
import org.springframework.batch.item.ItemKeyGenerator;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.exception.handler.ExceptionHandler;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.callback.ItemReaderRetryCallback;
import org.springframework.batch.retry.policy.ItemReaderRetryPolicy;
import org.springframework.batch.retry.support.RetryTemplate;

/**
 * @author Dave Syer
 * 
 */
public class KitchenSinkStepFactoryBean extends AbstractStepFactoryBean {

	private int commitInterval = 0;

	private Object[] listeners = new Object[0];

	private ItemSkipPolicy itemSkipPolicy = new NeverSkipItemSkipPolicy();

	private RepeatOperations stepOperations = new RepeatTemplate();

	private RetryPolicy retryPolicy = null;

	private ExceptionHandler exceptionHandler;

	private ItemKeyGenerator itemKeyGenerator;

	/**
	 * Set the commit interval.
	 * 
	 * @param commitInterval
	 */
	public void setCommitInterval(int commitInterval) {
		this.commitInterval = commitInterval;
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

	/**
	 * @param step
	 * 
	 */
	protected void applyConfiguration(ItemOrientedStep step) {
		
		super.applyConfiguration(step);
		
		step.setListeners(listeners);
		step.setItemSkipPolicy(itemSkipPolicy);

		if (retryPolicy != null) {
			ItemReaderRetryCallback retryCallback = new ItemReaderRetryCallback(getItemReader(), getKeyGenerator(),
					getItemWriter());
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
	 * @return an {@link ItemKeyGenerator} or null if none is found.
	 */
	private ItemKeyGenerator getKeyGenerator() {

		if (itemKeyGenerator != null) {
			return itemKeyGenerator;
		}
		if (getItemReader() instanceof ItemKeyGenerator) {
			return (ItemKeyGenerator) getItemReader();
		}
		if (getItemWriter() instanceof ItemKeyGenerator) {
			return (ItemKeyGenerator) getItemWriter();
		}
		return null;

	}

}
