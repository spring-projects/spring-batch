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

import org.jspecify.annotations.NullUnmarked;

import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.item.Chunk;

/**
 * Interface defined for processing {@link org.springframework.batch.item.Chunk}s.
 *
 * @author Kyeonghoon Lee (Add FunctionalInterface annotation)
 * @since 2.0
 * @deprecated Since 6.0 with no replacement. Scheduled for removal in 7.0.
 */
@NullUnmarked
@Deprecated(since = "6.0", forRemoval = true)
@FunctionalInterface
public interface ChunkProcessor<I> {

	void process(StepContribution contribution, Chunk<I> chunk) throws Exception;

}
