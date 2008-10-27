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
package org.springframework.batch.flow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;

/**
 * A {@link State} implementation that splits a {@link Flow} into multiple
 * parallel subflows.
 * 
 * @author Dave Syer
 * 
 */
public class SplitState<T> extends AbstractState<T> {

	private final Collection<Flow<T>> flows;

	private TaskExecutor taskExecutor = new SyncTaskExecutor();

	private FlowExecutionAggregator aggregator = new MaxValueFlowExecutionAggregator();

	/**
	 * @param name
	 */
	public SplitState(String name, Collection<Flow<T>> flows) {
		super(name);
		this.flows = flows;
	}

	/**
	 * Public setter for the taskExecutor.
	 * @param taskExecutor the taskExecutor to set
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Execute the flows in parallel by passing them to the {@link TaskExecutor}
	 * and wait for all of them to finish before proceeding.
	 * 
	 * @see State#handle(Object)
	 */
	@Override
	public String handle(final T context) throws Exception {

		Collection<FutureTask<FlowExecution>> tasks = new ArrayList<FutureTask<FlowExecution>>();

		for (final Flow<T> flow : flows) {

			final FutureTask<FlowExecution> task = new FutureTask<FlowExecution>(new Callable<FlowExecution>() {
				public FlowExecution call() throws Exception {
					return flow.start(context);
				}
			});

			tasks.add(task);

			try {
				taskExecutor.execute(new Runnable() {
					public void run() {
						task.run();
					}
				});
			}
			catch (TaskRejectedException e) {
				throw new FlowExecutionException("TaskExecutor rejected task for flow=" + flow.getName());
			}

		}

		Collection<FlowExecution> results = new ArrayList<FlowExecution>();

		// TODO: could use a CompletionSerice?
		for (FutureTask<FlowExecution> task : tasks) {
			results.add(task.get());
		}

		return aggregator.aggregate(results);

	}

}
