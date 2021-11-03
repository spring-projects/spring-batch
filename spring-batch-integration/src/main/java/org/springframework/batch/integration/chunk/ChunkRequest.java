/*
 * Copyright 2006-2007 the original author or authors.
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
import java.util.Collection;

import org.springframework.batch.core.StepContribution;

/**
 * Encapsulation of a chunk of items to be processed remotely as part of a step
 * execution.
 * 
 * @author Dave Syer
 * 
 * @param <T> the type of the items to process
 */
public class ChunkRequest<T> implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * The id of the job that the chunk is associated.
	 */
	private final long jobId;

	/**
	 * The collection of items that are to be processed.
	 */
	private final Collection<? extends T> items;

	/**
	 * Represents a contribution to a {@link org.springframework.batch.core.StepExecution}.
	 */
	private final StepContribution stepContribution;

	/**
	 * The sequence associated with the chunk.
	 */
	private final int sequence;

	/**
	 * @param sequence associated with the processed chunk.
	 * @param items collection items to be processed.
	 * @param jobId of the job that the chunk is associated.
	 * @param stepContribution represents a contribution to a {@link org.springframework.batch.core.StepExecution}
	 */
	public ChunkRequest(int sequence, Collection<? extends T> items, long jobId, StepContribution stepContribution) {
		this.sequence = sequence;
		this.items = items;
		this.jobId = jobId;
		this.stepContribution = stepContribution;
	}

	/**
	 * @return The id of the job.
	 */
	public long getJobId() {
		return jobId;
	}

	/**
	 * @return The items to be processed.
	 */
	public Collection<? extends T> getItems() {
		return items;
	}

	/**
	 * @return sequence associated with the processed chunk.
	 */
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
