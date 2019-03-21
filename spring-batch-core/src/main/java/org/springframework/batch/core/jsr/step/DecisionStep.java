/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.batch.core.jsr.step;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.batch.api.Decider;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.jsr.JsrStepExecution;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.item.ExecutionContext;

/**
 * Implements a {@link Step} to follow the rules for a decision state
 * as defined by JSR-352.  Currently does not support the JSR requirement
 * to provide all of the last {@link javax.batch.runtime.StepExecution}s from
 * a split.
 *
 * @author Michael Minella
 * @since 3.0
 */
public class DecisionStep extends AbstractStep {

	private final Decider decider;

	/**
	 * @param decider a {@link Decider} implementation
	 */
	public DecisionStep(Decider decider) {
		this.decider = decider;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void doExecute(StepExecution stepExecution) throws Exception {
		ExecutionContext executionContext = stepExecution.getJobExecution().getExecutionContext();
		List<javax.batch.runtime.StepExecution> stepExecutions = new ArrayList<>();

		if(executionContext.containsKey("batch.lastSteps")) {
			List<String> stepNames = (List<String>) executionContext.get("batch.lastSteps");

			for (String stepName : stepNames) {
				StepExecution curStepExecution = getJobRepository().getLastStepExecution(stepExecution.getJobExecution().getJobInstance(), stepName);
				stepExecutions.add(new JsrStepExecution(curStepExecution));
			}
		} else {
			Collection<StepExecution> currentRunStepExecutions = stepExecution.getJobExecution().getStepExecutions();

			StepExecution lastExecution = null;

			if(stepExecutions != null) {
				for (StepExecution curStepExecution : currentRunStepExecutions) {
					if(lastExecution == null || (curStepExecution.getEndTime() != null && curStepExecution.getEndTime().after(lastExecution.getEndTime()))) {
						lastExecution = curStepExecution;
					}
				}

				stepExecutions.add(new JsrStepExecution(lastExecution));
			}
		}

		try {
			ExitStatus exitStatus = new ExitStatus(decider.decide(stepExecutions.toArray(new javax.batch.runtime.StepExecution[0])));

			stepExecution.getJobExecution().setExitStatus(exitStatus);
			stepExecution.setExitStatus(exitStatus);

			if(executionContext.containsKey("batch.lastSteps")) {
				executionContext.remove("batch.lastSteps");
			}
		} catch (Exception e) {
			stepExecution.setTerminateOnly();
			stepExecution.addFailureException(e);
			throw e;
		}
	}
}
