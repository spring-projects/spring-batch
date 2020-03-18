/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.batch.core.jsr.job.flow.support.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.FlowExecutor;
import org.springframework.batch.core.jsr.job.flow.support.JsrFlow;

/**
 * JSR-352 states that artifacts cannot set the ExitStatus from within a split for a job.  Because
 * of this, this state will reset the exit status once the flows have completed (prior to aggregation
 * of the results).
 *
 * @author Michael Minella
 * @since 3.0
 */
public class JsrSplitState extends org.springframework.batch.core.job.flow.support.state.SplitState {

	/**
	 * @param flows {@link Flow}s to be executed in parallel
	 * @param name the name to be associated with the split state.
	 */
	public JsrSplitState(Collection<Flow> flows, String name) {
		super(flows, name);
	}

	/**
	 * Resets the {@link JobExecution}'s exit status before aggregating the results of the flows within
	 * the split.
	 *
	 * @param results the {@link FlowExecution}s from each of the flows executed within this split
	 * @param executor the {@link FlowExecutor} used to execute the flows
	 */
	@Override
	protected FlowExecutionStatus doAggregation(Collection<FlowExecution> results, FlowExecutor executor) {
		List<String> stepNames = new ArrayList<>();

		for (Flow curFlow : getFlows()) {
			JsrFlow flow = (JsrFlow) curFlow;
			if(flow.getMostRecentStepName() != null) {
				stepNames.add(flow.getMostRecentStepName());
			}
		}

		if(!stepNames.isEmpty()) {
			executor.getJobExecution().getExecutionContext().put("batch.lastSteps", stepNames);
		}

		executor.getJobExecution().setExitStatus(null);

		return super.doAggregation(results, executor);
	}
}
