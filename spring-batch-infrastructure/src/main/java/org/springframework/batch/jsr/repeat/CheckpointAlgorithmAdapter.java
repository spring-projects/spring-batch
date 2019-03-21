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
package org.springframework.batch.jsr.repeat;

import javax.batch.api.chunk.CheckpointAlgorithm;
import javax.batch.operations.BatchRuntimeException;

import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.util.Assert;

/**
 * Wrapper for the {@link CheckpointAlgorithm} to be used via the rest
 * of the framework.
 *
 * @author Michael Minella
 * @see CheckpointAlgorithm
 * @see CompletionPolicy
 */
public class CheckpointAlgorithmAdapter implements CompletionPolicy {

	private CheckpointAlgorithm policy;
	private boolean isComplete = false;

	public CheckpointAlgorithmAdapter(CheckpointAlgorithm policy) {
		Assert.notNull(policy, "A CheckpointAlgorithm is required");

		this.policy = policy;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.repeat.CompletionPolicy#isComplete(org.springframework.batch.repeat.RepeatContext, org.springframework.batch.repeat.RepeatStatus)
	 */
	@Override
	public boolean isComplete(RepeatContext context, RepeatStatus result) {
		try {
			isComplete = policy.isReadyToCheckpoint();
			return isComplete;
		} catch (Exception e) {
			throw new BatchRuntimeException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.repeat.CompletionPolicy#isComplete(org.springframework.batch.repeat.RepeatContext)
	 */
	@Override
	public boolean isComplete(RepeatContext context) {
		try {
			isComplete = policy.isReadyToCheckpoint();
			return isComplete;
		} catch (Exception e) {
			throw new BatchRuntimeException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.repeat.CompletionPolicy#start(org.springframework.batch.repeat.RepeatContext)
	 */
	@Override
	public RepeatContext start(RepeatContext parent) {
		try {
			policy.beginCheckpoint();
		} catch (Exception e) {
			throw new BatchRuntimeException(e);
		}

		return parent;
	}

	/**
	 * If {@link CheckpointAlgorithm#isReadyToCheckpoint()} is true
	 * we will call {@link CheckpointAlgorithm#endCheckpoint()}
	 *
	 * @param context a {@link RepeatContext}
	 */
	@Override
	public void update(RepeatContext context) {
		try {
			if(isComplete) {
				policy.endCheckpoint();
			}
		} catch (Exception e) {
			throw new BatchRuntimeException(e);
		}
	}
}
