/*
 * Copyright 2013-2019 the original author or authors.
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

import java.util.Collection;

import javax.batch.api.partition.PartitionReducer;
import javax.batch.api.partition.PartitionReducer.PartitionStatus;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.jsr.partition.JsrPartitionHandler;
import org.springframework.batch.core.jsr.partition.support.JsrStepExecutionAggregator;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.StepExecutionSplitter;
import org.springframework.batch.core.partition.support.StepExecutionAggregator;
import org.springframework.batch.core.step.NoSuchStepException;
import org.springframework.batch.core.step.StepLocator;
import org.springframework.batch.item.ExecutionContext;

/**
 * An extension of the {@link PartitionStep} that provides additional semantics
 * required by JSR-352.  Specifically, this implementation adds the required
 * lifecycle calls to the {@link PartitionReducer} if it is used.
 *
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @since 3.0
 */
public class PartitionStep extends org.springframework.batch.core.partition.support.PartitionStep implements StepLocator {

	private PartitionReducer reducer;
	private boolean hasReducer = false;
	private StepExecutionAggregator stepExecutionAggregator = new JsrStepExecutionAggregator();

	public void setPartitionReducer(PartitionReducer reducer) {
		this.reducer = reducer;
		hasReducer = reducer != null;
	}

	/**
	 * Delegate execution to the {@link PartitionHandler} provided. The
	 * {@link StepExecution} passed in here becomes the parent or manager
	 * execution for the partition, summarizing the status on exit of the
	 * logical grouping of work carried out by the {@link PartitionHandler}. The
	 * individual step executions and their input parameters (through
	 * {@link ExecutionContext}) for the partition elements are provided by the
	 * {@link StepExecutionSplitter}.
	 *
	 * @param stepExecution the manager step execution for the partition
	 *
	 * @see Step#execute(StepExecution)
	 */
	@Override
	protected void doExecute(StepExecution stepExecution) throws Exception {

		if(hasReducer) {
			reducer.beginPartitionedStep();
		}

		// Wait for task completion and then aggregate the results
		Collection<StepExecution> stepExecutions = getPartitionHandler().handle(null, stepExecution);
		stepExecution.upgradeStatus(BatchStatus.COMPLETED);
		stepExecutionAggregator.aggregate(stepExecution, stepExecutions);

		if (stepExecution.getStatus().isUnsuccessful()) {
			if (hasReducer) {
				reducer.rollbackPartitionedStep();
				reducer.afterPartitionedStepCompletion(PartitionStatus.ROLLBACK);
			}
			throw new JobExecutionException("Partition handler returned an unsuccessful step");
		}

		if (hasReducer) {
			reducer.beforePartitionedStepCompletion();
			reducer.afterPartitionedStepCompletion(PartitionStatus.COMMIT);
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.step.StepLocator#getStepNames()
	 */
	@Override
	public Collection<String> getStepNames() {
		return ((JsrPartitionHandler) getPartitionHandler()).getPartitionStepNames();
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.step.StepLocator#getStep(java.lang.String)
	 */
	@Override
	public Step getStep(String stepName) throws NoSuchStepException {
		JsrPartitionHandler partitionHandler =  (JsrPartitionHandler) getPartitionHandler();
		Collection<String> names = partitionHandler.getPartitionStepNames();

		if(names.contains(stepName)) {
			return partitionHandler.getStep();
		} else {
			throw new NoSuchStepException(stepName + " was not found");
		}
	}
}
