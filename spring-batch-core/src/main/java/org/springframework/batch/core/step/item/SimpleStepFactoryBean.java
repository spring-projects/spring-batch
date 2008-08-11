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
package org.springframework.batch.core.step.item;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Step;
import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.exception.DefaultExceptionHandler;
import org.springframework.batch.repeat.exception.ExceptionHandler;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.repeat.support.TaskExecutorRepeatTemplate;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;

/**
 * Most common configuration options for simple steps should be found here. Use
 * this factory bean instead of creating a {@link Step} implementation manually.
 * 
 * This factory does not support configuration of fault-tolerant behavior, use
 * appropriate subclass of this factory bean to configure skip or retry.
 * 
 * @see SkipLimitStepFactoryBean
 * 
 * @author Dave Syer
 * 
 */
public class SimpleStepFactoryBean<T,S> extends AbstractStepFactoryBean<T,S> {

	protected final Log logger = LogFactory.getLog(getClass());

	private static final int DEFAULT_COMMIT_INTERVAL = 1;

	private int commitInterval = 0;

	private TaskExecutor taskExecutor;

	private ItemHandler itemHandler;

	private RepeatTemplate stepOperations;

	private RepeatTemplate chunkOperations;

	private ExceptionHandler exceptionHandler = new DefaultExceptionHandler();

	private CompletionPolicy chunkCompletionPolicy;

	private int throttleLimit = TaskExecutorRepeatTemplate.DEFAULT_THROTTLE_LIMIT;

	/**
	 * Set the commit interval. Either set this or the chunkCompletionPolicy but
	 * not both.
	 * 
	 * @param commitInterval 1 by default
	 */
	public void setCommitInterval(int commitInterval) {
		this.commitInterval = commitInterval;
	}

	/**
	 * Public setter for the {@link CompletionPolicy} applying to the chunk
	 * level. A transaction will be committed when this policy decides to
	 * complete. Defaults to a {@link SimpleCompletionPolicy} with chunk size
	 * equal to the commitInterval property.
	 * 
	 * @param chunkCompletionPolicy the chunkCompletionPolicy to set
	 */
	public void setChunkCompletionPolicy(CompletionPolicy chunkCompletionPolicy) {
		this.chunkCompletionPolicy = chunkCompletionPolicy;
	}

	/**
	 * Protected getter for the step operations to make them available in
	 * subclasses.
	 * @return the step operations
	 */
	protected RepeatTemplate getStepOperations() {
		return stepOperations;
	}

	/**
	 * Protected getter for the chunk operations to make them available in
	 * subclasses.
	 * @return the step operations
	 */
	protected RepeatTemplate getChunkOperations() {
		return chunkOperations;
	}

	/**
	 * Public setter for the {@link ExceptionHandler}.
	 * @param exceptionHandler the exceptionHandler to set
	 */
	public void setExceptionHandler(ExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * Protected getter for the {@link ExceptionHandler}.
	 * @return the {@link ExceptionHandler}
	 */
	protected ExceptionHandler getExceptionHandler() {
		return exceptionHandler;
	}

	/**
	 * Public setter for the {@link TaskExecutor}. If this is set, then it will
	 * be used to execute the chunk processing inside the {@link Step}.
	 * 
	 * @param taskExecutor the taskExecutor to set
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Public setter for the throttle limit. This limits the number of tasks
	 * queued for concurrent processing to prevent thread pools from being
	 * overwhelmed. Defaults to
	 * {@link TaskExecutorRepeatTemplate#DEFAULT_THROTTLE_LIMIT}.
	 * @param throttleLimit the throttle limit to set.
	 */
	public void setThrottleLimit(int throttleLimit) {
		this.throttleLimit = throttleLimit;
	}

	/**
	 * Public getter for the ItemHandler.
	 * @return the ItemHandler
	 */
	protected ItemHandler getItemHandler() {
		return itemHandler;
	}

	/**
	 * Public setter for the ItemHandler.
	 * @param itemHandler the ItemHandler to set
	 */
	protected void setItemHandler(ItemHandler itemHandler) {
		this.itemHandler = itemHandler;
	}

	/**
	 * @param step
	 * 
	 */
	protected void applyConfiguration(ItemOrientedStep step) {

		super.applyConfiguration(step);

		chunkOperations = new RepeatTemplate();
		chunkOperations.setCompletionPolicy(getChunkCompletionPolicy());
		BatchListenerFactoryHelper.addChunkListeners(chunkOperations, getListeners());
		step.setChunkOperations(chunkOperations);

		stepOperations = new RepeatTemplate();

		if (taskExecutor != null) {
			TaskExecutorRepeatTemplate repeatTemplate = new TaskExecutorRepeatTemplate();
			repeatTemplate.setTaskExecutor(taskExecutor);
			repeatTemplate.setThrottleLimit(throttleLimit);
			stepOperations = repeatTemplate;
		}

		stepOperations.setExceptionHandler(exceptionHandler);

		step.setStepOperations(stepOperations);

	}

	/**
	 * @return a {@link CompletionPolicy} consistent with the commit interval
	 * and injected policy (if present).
	 */
	private CompletionPolicy getChunkCompletionPolicy() {
		Assert.state(!(chunkCompletionPolicy != null && commitInterval != 0),
				"You must specify either a chunkCompletionPolicy or a commitInterval but not both.");
		Assert.state(commitInterval >= 0, "The commitInterval must be positive or zero (for default value).");

		if (chunkCompletionPolicy != null) {
			return chunkCompletionPolicy;
		}
		if (commitInterval == 0) {
			logger.info("Setting commit interval to default value (" + DEFAULT_COMMIT_INTERVAL + ")");
			commitInterval = DEFAULT_COMMIT_INTERVAL;
		}
		return new SimpleCompletionPolicy(commitInterval);
	}

}
