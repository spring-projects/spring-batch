/*
 * Copyright 2013-2018 the original author or authors.
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

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.jsr.configuration.support.BatchPropertyContext;
import org.springframework.batch.core.jsr.step.BatchletStep;
import org.springframework.batch.core.step.builder.StepBuilderException;
import org.springframework.batch.core.step.builder.StepBuilderHelper;
import org.springframework.batch.core.step.builder.TaskletStepBuilder;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.repeat.support.TaskExecutorRepeatTemplate;

/**
 * Extension of the {@link TaskletStepBuilder} that uses a {@link BatchletStep} instead
 * of a {@link TaskletStep}.
 *
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @since 3.0
 */
public class JsrBatchletStepBuilder extends TaskletStepBuilder {

	private BatchPropertyContext batchPropertyContext;

	/**
	 * @param context used to resolve lazy bound properties
	 */
	public void setBatchPropertyContext(BatchPropertyContext context) {
		this.batchPropertyContext = context;
	}

	public JsrBatchletStepBuilder(StepBuilderHelper<? extends StepBuilderHelper<?>> parent) {
		super(parent);
	}

	/**
	 * Build the step from the components collected by the fluent setters. Delegates first to {@link #enhance(Step)} and
	 * then to {@link #createTasklet()} in subclasses to create the actual tasklet.
	 *
	 * @return a tasklet step fully configured and read to execute
	 */
	@Override
	public TaskletStep build() {

		registerStepListenerAsChunkListener();

		BatchletStep step = new BatchletStep(getName(), batchPropertyContext);

		super.enhance(step);

		step.setChunkListeners(chunkListeners.toArray(new ChunkListener[0]));

		if (getTransactionAttribute() != null) {
			step.setTransactionAttribute(getTransactionAttribute());
		}

		if (getStepOperations() == null) {

			stepOperations(new RepeatTemplate());

			if (getTaskExecutor() != null) {
				TaskExecutorRepeatTemplate repeatTemplate = new TaskExecutorRepeatTemplate();
				repeatTemplate.setTaskExecutor(getTaskExecutor());
				repeatTemplate.setThrottleLimit(getThrottleLimit());
				stepOperations(repeatTemplate);
			}

			((RepeatTemplate) getStepOperations()).setExceptionHandler(getExceptionHandler());

		}
		step.setStepOperations(getStepOperations());
		step.setTasklet(createTasklet());

		step.setStreams(getStreams().toArray(new ItemStream[0]));

		try {
			step.afterPropertiesSet();
		}
		catch (Exception e) {
			throw new StepBuilderException(e);
		}

		return step;

	}
}
