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

import org.springframework.batch.core.domain.BatchListener;
import org.springframework.batch.core.domain.Step;
import org.springframework.batch.core.domain.StepListener;
import org.springframework.batch.execution.step.ItemOrientedStep;
import org.springframework.batch.execution.step.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.exception.handler.SimpleLimitExceptionHandler;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.repeat.support.TaskExecutorRepeatTemplate;
import org.springframework.core.task.TaskExecutor;

/**
 * Most common configuration options for simple steps should be found here. Use
 * this factory bean instead of creating a {@link Step} implementation manually.
 * 
 * @author Dave Syer
 * 
 */
public class DefaultStepFactoryBean extends AbstractStepFactoryBean {

	private int skipLimit = 0;

	private int commitInterval = 0;

	private ItemStream[] streams = new ItemStream[0];

	private BatchListener[] listeners = new BatchListener[0];

	private ListenerMulticaster listener = new ListenerMulticaster();

	private TaskExecutor taskExecutor;

	private ItemProcessor itemProcessor;

	private RepeatTemplate stepOperations;

	/**
	 * Set the commit interval.
	 * 
	 * @param commitInterval
	 */
	public void setCommitInterval(int commitInterval) {
		this.commitInterval = commitInterval;
	}

	/**
	 * The streams to inject into the {@link Step}. Any instance of
	 * {@link ItemStream} can be used, and will then receive callbacks at the
	 * appropriate stage in the step.
	 * 
	 * @param streams an array of listeners
	 */
	public void setStreams(ItemStream[] streams) {
		this.streams = streams;
	}

	/**
	 * Public setter for a limit that determines skip policy. If this value is
	 * positive then an exception in chunk processing will cause the item to be
	 * skipped and no exception propagated until the limit is reached. If it is
	 * zero then all exceptions will be propagated from the chunk and cause the
	 * step to abort.
	 * 
	 * @param skipLimit the value to set. Default is 0 (never skip).
	 */
	public void setSkipLimit(int skipLimit) {
		this.skipLimit = skipLimit;
	}

	/**
	 * The listeners to inject into the {@link Step}. Any instance of
	 * {@link BatchListener} can be used, and will then receive callbacks at the
	 * appropriate stage in the step.
	 * 
	 * @param listeners an array of listeners
	 */
	public void setListeners(BatchListener[] listeners) {
		this.listeners = listeners;
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
	 * Public setter for the {@link TaskExecutor}. If this is set, then it will
	 * be used to execute the chunk processing inside the {@link Step}.
	 * 
	 * @param taskExecutor the taskExecutor to set
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Public getter for the ItemProcessor.
	 * @return the itemProcessor
	 */
	protected ItemProcessor getItemProcessor() {
		return itemProcessor;
	}

	/**
	 * Public setter for the ItemProcessor.
	 * @param itemProcessor the itemProcessor to set
	 */
	protected void setItemProcessor(ItemProcessor itemProcessor) {
		this.itemProcessor = itemProcessor;
	}

	/**
	 * @param step
	 * 
	 */
	protected void applyConfiguration(ItemOrientedStep step) {

		super.applyConfiguration(step);

		step.setStreams(streams);

		if (commitInterval > 0) {
			RepeatTemplate chunkOperations = new RepeatTemplate();
			chunkOperations.setCompletionPolicy(new SimpleCompletionPolicy(commitInterval));
			step.setChunkOperations(chunkOperations);
		}

		for (int i = 0; i < listeners.length; i++) {
			BatchListener listener = listeners[i];
			if (listener instanceof StepListener) {
				step.registerStepListener((StepListener) listener);
			}
			else {
				this.listener.register(listener);
			}
		}

		ItemReader itemReader = getItemReader();
		ItemWriter itemWriter = getItemWriter();

		// Since we are going to wrap these things with listener callbacks we
		// need to register them here because the step will not know we did
		// that.
		if (itemReader instanceof ItemStream) {
			step.registerStream((ItemStream) itemReader);
		}
		if (itemReader instanceof StepListener) {
			step.registerStepListener((StepListener) itemReader);
		}
		if (itemWriter instanceof ItemStream) {
			step.registerStream((ItemStream) itemWriter);
		}
		if (itemWriter instanceof StepListener) {
			step.registerStepListener((StepListener) itemWriter);
		}

		BatchListenerFactoryHelper helper = new BatchListenerFactoryHelper();

		StepListener[] stepListeners = helper.getStepListeners(listeners);
		itemReader = helper.getItemReader(itemReader, listeners);
		itemWriter = helper.getItemWriter(itemWriter, listeners);
		stepOperations = new RepeatTemplate();
		stepOperations = (RepeatTemplate) helper.getStepOperations(stepOperations, listeners);

		// In case they are used by subclasses:
		setItemReader(itemReader);
		setItemWriter(itemWriter);

		step.setStepListeners(stepListeners);

		if (taskExecutor != null) {
			TaskExecutorRepeatTemplate repeatTemplate = new TaskExecutorRepeatTemplate();
			repeatTemplate.setTaskExecutor(taskExecutor);
			stepOperations = repeatTemplate;
		}

		step.setStepOperations(stepOperations);

		ItemSkipPolicyItemProcessor itemProcessor = new ItemSkipPolicyItemProcessor(itemReader, itemWriter);

		if (skipLimit > 0) {
			/*
			 * If there is a skip limit (not the default) then we are prepared
			 * to absorb exceptions at the step level because the failed items
			 * will never re-appear after a rollback.
			 */
			itemProcessor.setItemSkipPolicy(new LimitCheckingItemSkipPolicy(skipLimit));
			stepOperations.setExceptionHandler(new SimpleLimitExceptionHandler(skipLimit));
			step.setStepOperations(stepOperations);
		}
		else {
			// This is the default in ItemOrientedStep anyway...
			itemProcessor.setItemSkipPolicy(new NeverSkipItemSkipPolicy());
		}

		setItemProcessor(itemProcessor);
		step.setItemProcessor(itemProcessor);

	}
	
}
