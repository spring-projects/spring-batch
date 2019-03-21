/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.step.builder;

import javax.batch.api.partition.PartitionReducer;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.jsr.step.PartitionStep;
import org.springframework.batch.core.partition.support.SimpleStepExecutionSplitter;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.step.builder.PartitionStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilderException;
import org.springframework.batch.core.step.builder.StepBuilderHelper;
import org.springframework.core.task.SyncTaskExecutor;

/**
 * An extension of the {@link PartitionStepBuilder} that uses {@link PartitionStep}
 * so that JSR-352 specific semantics are honored.
 *
 * @author Michael Minella
 * @since 3.0
 */
public class JsrPartitionStepBuilder extends PartitionStepBuilder {

	private PartitionReducer reducer;

	/**
	 * @param parent parent step builder for basic step properties
	 */
	public JsrPartitionStepBuilder(StepBuilderHelper<?> parent) {
		super(parent);
	}

	/**
	 * @param reducer used to provide a single callback at the beginning and end
	 * of a partitioned step.
	 *
	 * @return this
	 */
	public JsrPartitionStepBuilder reducer(PartitionReducer reducer) {
		this.reducer = reducer;
		return this;
	}

	@Override
	public JsrPartitionStepBuilder step(Step step) {
		super.step(step);
		return this;
	}

	@Override
	public Step build() {
		PartitionStep step = new PartitionStep();
		step.setName(getName());
		super.enhance(step);

		if (getPartitionHandler() != null) {
			step.setPartitionHandler(getPartitionHandler());
		}
		else {
			TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
			partitionHandler.setStep(getStep());
			if (getTaskExecutor() == null) {
				taskExecutor(new SyncTaskExecutor());
			}
			partitionHandler.setGridSize(getGridSize());
			partitionHandler.setTaskExecutor(getTaskExecutor());
			step.setPartitionHandler(partitionHandler);
		}

		if (getSplitter() != null) {
			step.setStepExecutionSplitter(getSplitter());
		}
		else {

			boolean allowStartIfComplete = isAllowStartIfComplete();
			String name = getStepName();
			if (getStep() != null) {
				try {
					allowStartIfComplete = getStep().isAllowStartIfComplete();
					name = getStep().getName();
				}
				catch (Exception e) {
					logger.info("Ignored exception from step asking for name and allowStartIfComplete flag. "
							+ "Using default from enclosing PartitionStep (" + name + "," + allowStartIfComplete + ").");
				}
			}
			SimpleStepExecutionSplitter splitter = new SimpleStepExecutionSplitter();
			splitter.setPartitioner(getPartitioner());
			splitter.setJobRepository(getJobRepository());
			splitter.setAllowStartIfComplete(allowStartIfComplete);
			splitter.setStepName(name);
			splitter(splitter);
			step.setStepExecutionSplitter(splitter);

		}

		if (getAggregator() != null) {
			step.setStepExecutionAggregator(getAggregator());
		}

		if(reducer != null) {
			step.setPartitionReducer(reducer);
		}

		try {
			step.afterPropertiesSet();
		}
		catch (Exception e) {
			throw new StepBuilderException(e);
		}

		return step;

	}
}
