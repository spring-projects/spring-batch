/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.step.builder;

import java.util.ArrayList;

import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.jsr.step.item.JsrChunkProcessor;
import org.springframework.batch.core.jsr.step.item.JsrChunkProvider;
import org.springframework.batch.core.step.builder.FaultTolerantStepBuilder;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.item.ChunkOrientedTasklet;
import org.springframework.batch.core.step.item.ChunkProcessor;
import org.springframework.batch.core.step.item.ChunkProvider;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.util.Assert;

/**
 * A step builder that extends the {@link FaultTolerantStepBuilder} to create JSR-352
 * specific {@link ChunkProvider} and {@link ChunkProcessor} supporting the chunking
 * pattern defined by the spec.
 *
 * @author Michael Minella
 *
 * @param <I> The input type for the step
 * @param <O> The output type for the step
 */
public class JsrSimpleStepBuilder<I, O> extends SimpleStepBuilder<I, O> {

	public JsrSimpleStepBuilder(StepBuilder parent) {
		super(parent);
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	protected Tasklet createTasklet() {
		Assert.state(getReader() != null, "ItemReader must be provided");
		Assert.state(getProcessor() != null || getWriter() != null, "ItemWriter or ItemProcessor must be provided");
		RepeatOperations repeatOperations = createRepeatOperations();
		ChunkProvider<I> chunkProvider = new JsrChunkProvider<I>();
		JsrChunkProcessor<I, O> chunkProcessor = new JsrChunkProcessor(getReader(), getProcessor(), getWriter(), repeatOperations);
		chunkProcessor.setListeners(new ArrayList<StepListener>(getItemListeners()));
		ChunkOrientedTasklet<I> tasklet = new ChunkOrientedTasklet<I>(chunkProvider, chunkProcessor);
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
