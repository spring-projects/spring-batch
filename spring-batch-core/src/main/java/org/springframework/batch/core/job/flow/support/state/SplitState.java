/*
 * Copyright 2006-2024 the original author or authors.
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
package org.springframework.batch.core.job.flow.support.state;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowExecution;
import org.springframework.batch.core.job.flow.FlowExecutionException;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.FlowExecutor;
import org.springframework.batch.core.job.flow.FlowHolder;
import org.springframework.batch.core.job.flow.State;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.lang.Nullable;

/**
 * A {@link State} implementation that splits a {@link Flow} into multiple parallel
 * subflows.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @since 2.0
 */
public class SplitState extends AbstractState implements FlowHolder {

	private final Collection<Flow> flows;

	private final SplitState parentSplit;

	private TaskExecutor taskExecutor = new SyncTaskExecutor();

	private final FlowExecutionAggregator aggregator = new MaxValueFlowExecutionAggregator();

	/**
	 * @param flows collection of {@link Flow} instances.
	 * @param name the name of the state.
	 */
	public SplitState(Collection<Flow> flows, String name) {
		this(flows, name, null);
	}

	/**
	 * @param flows collection of {@link Flow} instances.
	 * @param name the name of the state.
	 * @param parentSplit the parent {@link SplitState}.
	 */
	public SplitState(Collection<Flow> flows, String name, @Nullable SplitState parentSplit) {
		super(name);
		this.flows = flows;
		this.parentSplit = parentSplit;
	}

	/**
	 * Public setter for the taskExecutor.
	 * @param taskExecutor the taskExecutor to set
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * @return the flows
	 */
	@Override
	public Collection<Flow> getFlows() {
		return flows;
	}

	/**
	 * Execute the flows in parallel by passing them to the {@link TaskExecutor} and wait
	 * for all of them to finish before proceeding.
	 *
	 * @see State#handle(FlowExecutor)
	 */
	@Override
	public FlowExecutionStatus handle(final FlowExecutor executor) throws Exception {

		// TODO: collect the last StepExecution from the flows as well, so they
		// can be abandoned if necessary
		Collection<Future<FlowExecution>> tasks = new ArrayList<>();

		for (final Flow flow : flows) {

			final FutureTask<FlowExecution> task = new FutureTask<>(() -> flow.start(executor));

			tasks.add(task);

			try {
				taskExecutor.execute(task);
			}
			catch (TaskRejectedException e) {
				throw new FlowExecutionException("TaskExecutor rejected task for flow=" + flow.getName());
			}

		}

		FlowExecutionStatus parentSplitStatus = parentSplit == null ? null : parentSplit.handle(executor);

		Collection<FlowExecution> results = new ArrayList<>();
		List<Exception> exceptions = new ArrayList<>();
		// Could use a CompletionService here?
		for (Future<FlowExecution> task : tasks) {
			try {
				results.add(task.get());
			}
			catch (ExecutionException e) {
				// Unwrap the expected exceptions
				Throwable cause = e.getCause();
				if (cause instanceof Exception exception) {
					exceptions.add(exception);
				}
				else {
					exceptions.add(e);
				}
			}
		}

		if (!exceptions.isEmpty()) {
			throw exceptions.get(0);
		}

		FlowExecutionStatus flowExecutionStatus = doAggregation(results, executor);
		if (parentSplitStatus != null) {
			return Collections.max(Arrays.asList(flowExecutionStatus, parentSplitStatus));
		}
		return flowExecutionStatus;
	}

	protected FlowExecutionStatus doAggregation(Collection<FlowExecution> results, FlowExecutor executor) {
		return aggregator.aggregate(results);
	}

	@Override
	public boolean isEndState() {
		return false;
	}

}
