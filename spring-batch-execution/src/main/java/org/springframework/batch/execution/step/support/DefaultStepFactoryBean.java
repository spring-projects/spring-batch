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
import org.springframework.batch.execution.step.ItemOrientedStep;
import org.springframework.batch.repeat.exception.handler.SimpleLimitExceptionHandler;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.repeat.support.TaskExecutorRepeatTemplate;
import org.springframework.core.task.TaskExecutor;

/**
 * Adds listeners to {@link SimpleStepFactoryBean}.
 * 
 * @author Dave Syer
 * 
 */
public class DefaultStepFactoryBean extends SimpleStepFactoryBean {

	private boolean alwaysSkip = false;

	private BatchListener[] listeners = new BatchListener[0];

	private TaskExecutor taskExecutor;

	/**
	 * Public setter for the flag that determines skip policy. If this flag is
	 * true then an exception in chunk processing will cause the item to be
	 * skipped and no exceptions propagated. If it is false then all exceptions
	 * will be propagated from the chunk and cause the step to abort.
	 * 
	 * @param alwaysSkip the value to set. Default is false.
	 */
	public void setAlwaysSkip(boolean alwaysSkip) {
		this.alwaysSkip = alwaysSkip;
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
	 * Public setter for the {@link TaskExecutor}. If this is set, then it will
	 * be used to execute the chunk processing inside the {@link Step}.
	 * 
	 * @param taskExecutor the taskExecutor to set
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * @param step
	 * 
	 */
	protected void applyConfiguration(ItemOrientedStep step) {

		super.applyConfiguration(step);
		step.setListeners(listeners);

		RepeatTemplate stepOperations = new RepeatTemplate();

		if (taskExecutor != null) {
			TaskExecutorRepeatTemplate repeatTemplate = new TaskExecutorRepeatTemplate();
			repeatTemplate.setTaskExecutor(taskExecutor);
			stepOperations = repeatTemplate;
		}

		if (alwaysSkip) {
			// If we always skip (not the default) then we are prepared to
			// absorb all exceptions at the step level because the failed items
			// will never re-appear after a rollback.
			step.setItemSkipPolicy(new AlwaysSkipItemSkipPolicy());
			stepOperations.setExceptionHandler(new SimpleLimitExceptionHandler(Integer.MAX_VALUE));
			step.setStepOperations(stepOperations);
		}
		else {
			// This is the default in ItemOrientedStep anyway...
			step.setItemSkipPolicy(new NeverSkipItemSkipPolicy());
		}

	}
}
