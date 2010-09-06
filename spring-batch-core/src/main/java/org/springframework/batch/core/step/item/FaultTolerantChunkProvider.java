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

import org.springframework.batch.classify.BinaryExceptionClassifier;
import org.springframework.batch.classify.Classifier;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.skip.LimitCheckingItemSkipPolicy;
import org.springframework.batch.core.step.skip.NonSkippableReadException;
import org.springframework.batch.core.step.skip.SkipException;
import org.springframework.batch.core.step.skip.SkipListenerFailedException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.core.step.skip.SkipPolicyFailedException;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.repeat.RepeatOperations;

/**
 * FaultTolerant implementation of the {@link ChunkProcessor} interface, that
 * allows for skipping or retry of items that cause exceptions during reading or
 * processing.
 * 
 */
public class FaultTolerantChunkProvider<I> extends SimpleChunkProvider<I> {

	/**
	 * Hard limit for number of read skips in the same chunk. Should be
	 * sufficiently high that it is only encountered in a runaway step where all
	 * items are skipped before the chunk can complete (leading to a potential
	 * heap memory problem).
	 */
	public static final int DEFAULT_MAX_SKIPS_ON_READ = 100;

	private SkipPolicy skipPolicy = new LimitCheckingItemSkipPolicy();

	private Classifier<Throwable, Boolean> rollbackClassifier = new BinaryExceptionClassifier(true);

	private int maxSkipsOnRead = DEFAULT_MAX_SKIPS_ON_READ;

	public FaultTolerantChunkProvider(ItemReader<? extends I> itemReader, RepeatOperations repeatOperations) {
		super(itemReader, repeatOperations);
	}
	
	/**
	 * @param maxSkipsOnRead the maximum number of skips on read
	 */
	public void setMaxSkipsOnRead(int maxSkipsOnRead) {
		this.maxSkipsOnRead = maxSkipsOnRead;
	}

	/**
	 * The policy that determines whether exceptions can be skipped on read.
	 * @param SkipPolicy
	 */
	public void setSkipPolicy(SkipPolicy SkipPolicy) {
		this.skipPolicy = SkipPolicy;
	}

	/**
	 * Classifier to determine whether exceptions have been marked as
	 * no-rollback (as opposed to skippable). If ecnounterd they are simply
	 * ignored, unless also skippable.
	 * 
	 * @param rollbackClassifier the rollback classifier to set
	 */
	public void setRollbackClassifier(Classifier<Throwable, Boolean> rollbackClassifier) {
		this.rollbackClassifier = rollbackClassifier;
	}

	@Override
	protected I read(StepContribution contribution, Chunk<I> chunk) throws Exception {
		while (true) {
			try {
				return doRead();
			}
			catch (Exception e) {

				if (shouldSkip(skipPolicy, e, contribution.getStepSkipCount())) {
					// increment skip count and try again
					contribution.incrementReadSkipCount();
					chunk.skip(e);

					if (chunk.getErrors().size() >= maxSkipsOnRead) {
						throw new SkipOverflowException("Too many skips on read");
					}

					logger.debug("Skipping failed input", e);
				}
				else {
					if (rollbackClassifier.classify(e)) {
						throw new NonSkippableReadException("Non-skippable exception during read", e);
					}
					logger.debug("No-rollback for non-skippable exception (ignored)", e);
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

	/**
	 * Convenience method for calling process skip policy.
	 * 
	 * @param policy the skip policy
	 * @param e the cause of the skip
	 * @param skipCount the current skip count
	 */
	private boolean shouldSkip(SkipPolicy policy, Throwable e, int skipCount) {
		try {
			return policy.shouldSkip(e, skipCount);
		}
		catch (SkipException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new SkipPolicyFailedException("Fatal exception in SkipPolicy.", ex, e);
		}
	}

}
