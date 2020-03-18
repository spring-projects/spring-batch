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
package org.springframework.batch.core.jsr.step.item;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.item.Chunk;
import org.springframework.batch.core.step.item.ChunkProvider;

/**
 * A no-op {@link ChunkProvider}.  The JSR-352 chunking model does not cache the
 * input as the regular Spring Batch implementations do so this component is not
 * needed within a chunking loop.
 *
 * @author Michael Minella
 *
 * @param <T> The type of input for the step
 */
public class JsrChunkProvider<T> implements ChunkProvider<T> {

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.step.item.ChunkProvider#provide(org.springframework.batch.core.StepContribution)
	 */
	@Override
	public Chunk<T> provide(StepContribution contribution) throws Exception {
		return new Chunk<>();
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.step.item.ChunkProvider#postProcess(org.springframework.batch.core.StepContribution, org.springframework.batch.core.step.item.Chunk)
	 */
	@Override
	public void postProcess(StepContribution contribution, Chunk<T> chunk) {
	}
}
