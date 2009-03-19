/*
 * Copyright 2006-2007 the original author or authors.
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

package org.springframework.batch.core.step.item;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.skip.LimitCheckingItemSkipPolicy;
import org.springframework.batch.core.step.skip.NonSkippableReadException;
import org.springframework.batch.core.step.skip.SkipListenerFailedException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.repeat.RepeatOperations;

public class FaultTolerantChunkProvider<I> extends SimpleChunkProvider<I> {

	private SkipPolicy skipPolicy = new LimitCheckingItemSkipPolicy(0);

	public FaultTolerantChunkProvider(ItemReader<? extends I> itemReader, RepeatOperations repeatOperations) {
		super(itemReader, repeatOperations);
	}

	/**
	 * The policy that determines whether exceptions can be skipped on read.
	 * @param SkipPolicy
	 */
	public void setSkipPolicy(SkipPolicy SkipPolicy) {
		this.skipPolicy = SkipPolicy;
	}

	@Override
	protected I read(StepContribution contribution, Chunk<I> chunk) throws Exception {
		while (true) {
			try {
				return doRead();
			}
			catch (Exception e) {

				if (skipPolicy.shouldSkip(e, contribution.getStepSkipCount())) {
					// increment skip count and try again
					contribution.incrementReadSkipCount();
					chunk.skip(e);

					logger.debug("Skipping failed input", e);
				}
				else {
					throw new NonSkippableReadException("Non-skippable exception during read", e);
				}

			}
		}
	}

	@Override
	public void postProcess(StepContribution contribution, Chunk<I> chunk) {
		for (Exception e : chunk.getErrors()) {
			try {
				getListener().onSkipInRead(e);
			}
			catch (RuntimeException ex) {
				throw new SkipListenerFailedException("Fatal exception in SkipListener.", ex, e);
			}
		}
	}

}
