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
package org.springframework.batch.core.jsr.step.builder;

import java.util.ArrayList;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.jsr.configuration.support.BatchPropertyContext;
import org.springframework.batch.core.jsr.step.BatchletStep;
import org.springframework.batch.core.jsr.step.item.JsrChunkProcessor;
import org.springframework.batch.core.jsr.step.item.JsrChunkProvider;
import org.springframework.batch.core.step.builder.FaultTolerantStepBuilder;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.builder.StepBuilderException;
import org.springframework.batch.core.step.item.ChunkOrientedTasklet;
import org.springframework.batch.core.step.item.ChunkProcessor;
import org.springframework.batch.core.step.item.ChunkProvider;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.repeat.support.TaskExecutorRepeatTemplate;
import org.springframework.util.Assert;

/**
 * A step builder that extends the {@link FaultTolerantStepBuilder} to create JSR-352
 * specific {@link ChunkProvider} and {@link ChunkProcessor} supporting the chunking
 * pattern defined by the spec.
 *
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 *
 * @param <I> The input type for the step
 * @param <O> The output type for the step
 */
public class JsrSimpleStepBuilder<I, O> extends SimpleStepBuilder<I, O> {

	private BatchPropertyContext batchPropertyContext;

	public JsrSimpleStepBuilder(StepBuilder parent) {
		super(parent);
	}

	public JsrPartitionStepBuilder partitioner(Step step) {
		return new JsrPartitionStepBuilder(this).step(step);
	}

	public void setBatchPropertyContext(BatchPropertyContext batchPropertyContext) {
		this.batchPropertyContext = batchPropertyContext;
	}

	/**
	 * Build the step from the components collected by the fluent setters. Delegates first to {@link #enhance(Step)} and
	 * then to {@link #createTasklet()} in subclasses to create the actual tasklet.
	 *
	 * @return a tasklet step fully configured and read to execute
	 */
	@Override
	public TaskletStep build() {
		registerStepListenerAsItemListener();
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

		ItemStream[] streams = getStreams().toArray(new ItemStream[0]);
		step.setStreams(streams);

		try {
			step.afterPropertiesSet();
		}
		catch (Exception e) {
			throw new StepBuilderException(e);
		}

		return step;

	}

	@Override
	protected Tasklet createTasklet() {
		Assert.state(getReader() != null, "ItemReader must be provided");
		Assert.state(getProcessor() != null || getWriter() != null, "ItemWriter or ItemProcessor must be provided");
		RepeatOperations repeatOperations = createRepeatOperations();
		ChunkProvider<I> chunkProvider = new JsrChunkProvider<>();
		JsrChunkProcessor<I, O> chunkProcessor = new JsrChunkProcessor<>(getReader(), getProcessor(), getWriter(), repeatOperations);
		chunkProcessor.setListeners(new ArrayList<>(getItemListeners()));
		ChunkOrientedTasklet<I> tasklet = new ChunkOrientedTasklet<>(chunkProvider, chunkProcessor);
		tasklet.setBuffering(!isReaderTransactionalQueue());
		return tasklet;
	}

	private RepeatOperations createRepeatOperations() {
		RepeatTemplate repeatOperations = new RepeatTemplate();
		repeatOperations.setCompletionPolicy(getChunkCompletionPolicy());
		repeatOperations.setExceptionHandler(getExceptionHandler());
		return repeatOperations;
	}
}
