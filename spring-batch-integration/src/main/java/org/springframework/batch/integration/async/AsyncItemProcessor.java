/*
 * Copyright 2006-2019 the original author or authors.
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
package org.springframework.batch.integration.async;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * An {@link ItemProcessor} that delegates to a nested processor and in the
 * background. To allow for background processing the return value from the
 * processor is a {@link Future} which needs to be unpacked before the item can
 * be used by a client.
 *
 * Because the {@link Future} is typically unwrapped in the {@link ItemWriter},
 * there are lifecycle and stats limitations (since the framework doesn't know
 * what the result of the processor is).  While not an exhaustive list, things like
 * {@link StepExecution#filterCount} will not reflect the number of filtered items
 * and {@link org.springframework.batch.core.ItemProcessListener#onProcessError(Object, Exception)}
 * will not be called.
 * 
 * @author Dave Syer
 * 
 * @param <I> the input object type
 * @param <O> the output object type (will be wrapped in a Future)
 * @see AsyncItemWriter
 */
public class AsyncItemProcessor<I, O> implements ItemProcessor<I, Future<O>>, InitializingBean {

	private ItemProcessor<I, O> delegate;

	private TaskExecutor taskExecutor = new SyncTaskExecutor();

	/**
	 * Check mandatory properties (the {@link #setDelegate(ItemProcessor)}).
	 * 
	 * @see InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(delegate, "The delegate must be set.");
	}

	/**
	 * The {@link ItemProcessor} to use to delegate processing to in a
	 * background thread.
	 * 
	 * @param delegate the {@link ItemProcessor} to use as a delegate
	 */
	public void setDelegate(ItemProcessor<I, O> delegate) {
		this.delegate = delegate;
	}

	/**
	 * The {@link TaskExecutor} to use to allow the item processing to proceed
	 * in the background. Defaults to a {@link SyncTaskExecutor} so no threads
	 * are created unless this is overridden.
	 * 
	 * @param taskExecutor a {@link TaskExecutor}
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Transform the input by delegating to the provided item processor. The
	 * return value is wrapped in a {@link Future} so that clients can unpack it
	 * later.
	 * 
	 * @see ItemProcessor#process(Object)
	 */
	@Nullable
	public Future<O> process(final I item) throws Exception {
		final StepExecution stepExecution = getStepExecution();
		FutureTask<O> task = new FutureTask<>(new Callable<O>() {
			public O call() throws Exception {
				if (stepExecution != null) {
					StepSynchronizationManager.register(stepExecution);
				}
				try {
					return delegate.process(item);
				}
				finally {
					if (stepExecution != null) {
						StepSynchronizationManager.close();
					}
				}
			}
		});
		taskExecutor.execute(task);
		return task;
	}

	/**
	 * @return the current step execution if there is one
	 */
	private StepExecution getStepExecution() {
		StepContext context = StepSynchronizationManager.getContext();
		if (context==null) {
			return null;
		}
		StepExecution stepExecution = context.getStepExecution();
		return stepExecution;
	}

}
