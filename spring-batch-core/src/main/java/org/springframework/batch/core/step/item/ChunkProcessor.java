/*
 * Copyright 2006-2025 the original author or authors.
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

package org.springframework.batch.core.step.item;

import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.infrastructure.item.Chunk;

/**
 * Interface defined for processing {@link Chunk}s.
 *
 * @author Mahmoud Ben Hassine
 * @author Kyeonghoon Lee (Add FunctionalInterface annotation)
 * @since 2.0
 */
@FunctionalInterface
public interface ChunkProcessor<I> {

	@Deprecated(since = "6.0", forRemoval = true)
	default void process(StepContribution contribution, Chunk<I> chunk) throws Exception {
		process(chunk, contribution);
	}

	/**
	 * Process the given chunk and update the contribution.
	 * @param chunk the chunk to process
	 * @param contribution the current step contribution
	 * @throws Exception if there is any error during processing
	 * @since 6.0
	 */
	void process(Chunk<I> chunk, StepContribution contribution) throws Exception;

}
