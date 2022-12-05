/*
 * Copyright 2006-2022 the original author or authors.
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

package org.springframework.batch.integration.chunk;

import java.io.Serializable;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.item.Chunk;

/**
 * Encapsulation of a chunk of items to be processed remotely as part of a step execution.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @param <T> the type of the items to process
 */
public class ChunkRequest<T> implements Serializable {

	private static final long serialVersionUID = 1L;

	private final long jobId;

	private final Chunk<? extends T> items;

	private final StepContribution stepContribution;

	private final int sequence;

	public ChunkRequest(int sequence, Chunk<? extends T> items, long jobId, StepContribution stepContribution) {
		this.sequence = sequence;
		this.items = items;
		this.jobId = jobId;
		this.stepContribution = stepContribution;
	}

	public long getJobId() {
		return jobId;
	}

	public Chunk<? extends T> getItems() {
		return items;
	}

	public int getSequence() {
		return sequence;
	}

	/**
	 * @return the {@link StepContribution} for this chunk
	 */
	public StepContribution getStepContribution() {
		return stepContribution;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName() + ": jobId=" + jobId + ", sequence=" + sequence + ", contribution="
				+ stepContribution + ", item count=" + items.size();
	}

}
