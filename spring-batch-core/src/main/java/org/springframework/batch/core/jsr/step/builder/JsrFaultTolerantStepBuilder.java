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
import org.springframework.batch.core.jsr.step.item.JsrChunkProvider;
import org.springframework.batch.core.jsr.step.item.JsrFaultTolerantChunkProcessor;
import org.springframework.batch.core.step.builder.FaultTolerantStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.item.ChunkOrientedTasklet;
import org.springframework.batch.core.step.item.ChunkProcessor;
import org.springframework.batch.core.step.item.ChunkProvider;
import org.springframework.batch.core.step.skip.SkipPolicy;

/**
 * A step builder that extends the {@link FaultTolerantStepBuilder} to create JSR-352
 * specific {@link ChunkProvider} and {@link ChunkProcessor} supporting both the chunking
 * pattern defined by the spec as well as skip/retry logic.
 *
 * @author Michael Minella
 *
 * @param <I> The input type for the step
 * @param <O> The output type for the step
 */
public class JsrFaultTolerantStepBuilder<I, O> extends FaultTolerantStepBuilder<I, O> {

	public JsrFaultTolerantStepBuilder(StepBuilder parent) {
		super(parent);
	}

	@Override
	public FaultTolerantStepBuilder<I, O> faultTolerant() {
		return this;
	}

	@Override
	protected ChunkProvider<I> createChunkProvider() {
		return new JsrChunkProvider<I>();
	}

	/**
	 * Provides a JSR-352 specific implementation of a {@link ChunkProcessor} for use
	 * within the {@link ChunkOrientedTasklet}
	 *
	 * @return a JSR-352 implementation of the {@link ChunkProcessor}
	 * @see {@link JsrFaultTolerantChunkProcessor}
	 */
	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	protected ChunkProcessor<I> createChunkProcessor() {
		SkipPolicy skipPolicy = getFatalExceptionAwareProxy(createSkipPolicy());
		JsrFaultTolerantChunkProcessor chunkProcessor = new JsrFaultTolerantChunkProcessor(getReader(), getProcessor(),
				getWriter(), createChunkOperations(), createRetryOperations());
		chunkProcessor.setSkipPolicy(skipPolicy);
		chunkProcessor.setRollbackClassifier(getRollbackClassifier());
		detectStreamInReader();
		chunkProcessor.setChunkMonitor(getChunkMonitor());
		ArrayList<StepListener> listeners = new ArrayList<StepListener>(getItemListeners());
		chunkProcessor.setListeners(listeners);

		return chunkProcessor;
	}
}
