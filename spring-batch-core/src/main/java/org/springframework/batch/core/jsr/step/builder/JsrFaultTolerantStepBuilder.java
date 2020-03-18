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

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.jsr.configuration.support.BatchPropertyContext;
import org.springframework.batch.core.jsr.step.BatchletStep;
import org.springframework.batch.core.jsr.step.item.JsrChunkProvider;
import org.springframework.batch.core.jsr.step.item.JsrFaultTolerantChunkProcessor;
import org.springframework.batch.core.step.builder.FaultTolerantStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.builder.StepBuilderException;
import org.springframework.batch.core.step.item.ChunkOrientedTasklet;
import org.springframework.batch.core.step.item.ChunkProcessor;
import org.springframework.batch.core.step.item.ChunkProvider;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.repeat.support.TaskExecutorRepeatTemplate;

/**
 * A step builder that extends the {@link FaultTolerantStepBuilder} to create JSR-352
 * specific {@link ChunkProvider} and {@link ChunkProcessor} supporting both the chunking
 * pattern defined by the spec as well as skip/retry logic.
 *
 * @author Michael Minella
 * @author Chris Schaefer
 *
 * @param <I> The input type for the step
 * @param <O> The output type for the step
 */
public class JsrFaultTolerantStepBuilder<I, O> extends FaultTolerantStepBuilder<I, O> {

	private BatchPropertyContext batchPropertyContext;

	public void setBatchPropertyContext(BatchPropertyContext batchPropertyContext) {
		this.batchPropertyContext = batchPropertyContext;
	}

	public JsrFaultTolerantStepBuilder(StepBuilder parent) {
		super(parent);
	}

	@Override
	public FaultTolerantStepBuilder<I, O> faultTolerant() {
		return this;
	}


	/**
	 * Build the step from the components collected by the fluent setters. Delegates first to {@link #enhance(Step)} and
	 * then to {@link #createTasklet()} in subclasses to create the actual tasklet.
	 *
	 * @return a tasklet step fully configured and read to execute
	 */
	@Override
	public TaskletStep build() {
		registerStepListenerAsSkipListener();
		registerAsStreamsAndListeners(getReader(), getProcessor(), getWriter());

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

	@Override
	protected ChunkProvider<I> createChunkProvider() {
		return new JsrChunkProvider<>();
	}

	/**
	 * Provides a JSR-352 specific implementation of a {@link ChunkProcessor} for use
	 * within the {@link ChunkOrientedTasklet}
	 *
	 * @return a JSR-352 implementation of the {@link ChunkProcessor}
	 * @see JsrFaultTolerantChunkProcessor
	 */
	@Override
	protected ChunkProcessor<I> createChunkProcessor() {
		SkipPolicy skipPolicy = getFatalExceptionAwareProxy(createSkipPolicy());
		JsrFaultTolerantChunkProcessor<I, O> chunkProcessor = 
				new JsrFaultTolerantChunkProcessor<>(getReader(), getProcessor(),
				getWriter(), createChunkOperations(), createRetryOperations());
		chunkProcessor.setSkipPolicy(skipPolicy);
		chunkProcessor.setRollbackClassifier(getRollbackClassifier());
		detectStreamInReader();
		chunkProcessor.setChunkMonitor(getChunkMonitor());
		chunkProcessor.setListeners(getChunkListeners());

		return chunkProcessor;
	}

	private List<StepListener> getChunkListeners() {
		List<StepListener> listeners = new ArrayList<>();
		listeners.addAll(getItemListeners());
		listeners.addAll(getSkipListeners());
		listeners.addAll(getJsrRetryListeners());

		return listeners;
	}
}
