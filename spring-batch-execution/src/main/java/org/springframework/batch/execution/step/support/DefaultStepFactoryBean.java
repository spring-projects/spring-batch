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
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.reader.DelegatingItemReader;
import org.springframework.batch.item.writer.DelegatingItemWriter;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.exception.handler.SimpleLimitExceptionHandler;
import org.springframework.batch.repeat.listener.RepeatListenerSupport;
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

	private ListenerMulticaster listener = new ListenerMulticaster();

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

		itemReader = new DelegatingItemReader(itemReader) {
			public Object read() throws Exception {
				try {
					listener.beforeRead();
					Object item = super.read();
					listener.afterRead(item);
					return item;
				}
				catch (Exception e) {
					listener.onReadError(e);
					throw e;
				}
			}
		};
		// In case it is used by subclasses:
		setItemReader(itemReader);
		step.setItemReader(itemReader);

		itemWriter = new DelegatingItemWriter(itemWriter) {
			public void write(Object item) throws Exception {
				try {
					listener.beforeWrite(item);
					super.write(item);
					listener.afterWrite();
				}
				catch (Exception e) {
					listener.onWriteError(e, item);
					throw e;
				}
			}
		};
		// In case it is used by subclasses:
		setItemWriter(itemWriter);
		step.setItemWriter(itemWriter);

		RepeatTemplate stepOperations = new RepeatTemplate();
		stepOperations.setListener(new RepeatListenerSupport() {
			public void open(RepeatContext context) {
				listener.beforeChunk();
			}

			public void close(RepeatContext context) {
				listener.afterChunk();
			}
		});

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
