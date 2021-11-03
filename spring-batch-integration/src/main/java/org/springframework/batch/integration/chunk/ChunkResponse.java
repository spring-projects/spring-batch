/*
 * Copyright 2006-2018 the original author or authors.
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
import org.springframework.lang.Nullable;

/**
 * Encapsulates a response to processing a chunk of items, summarising the result as a {@link StepContribution}.
 * 
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * 
 */
public class ChunkResponse implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Represents a contribution to the {@link org.springframework.batch.core.StepExecution}.
	 */
	private final StepContribution stepContribution;

	/**
	 * The id of the job that the chunk is associated.
	 */
	private final Long jobId;

	/**
	 * The result of chunk processing.  True if successful, else it is set to false.
	 */
	private final boolean status;

	/**
	 * Informational message indicating the state of the response.
	 */
	private final String message;

	/**
	 * If true indicates the chunk was redelivered for processing.
	 */
	private final boolean redelivered;

	/**
	 * The sequence associated with the chunk.
	 */
	private final int sequence;

	/**
	 * @param sequence associated with the processed chunk.
	 * @param jobId of the job that the chunk is associated.
	 * @param stepContribution represents a contribution to a {@link org.springframework.batch.core.StepExecution}
	 */
	public ChunkResponse(int sequence, Long jobId, StepContribution stepContribution) {
		this(true, sequence, jobId, stepContribution, null);
	}

	/**
	 * @param status the result of chunk processing.  True if successful, else it is set to false.
	 * @param sequence associated with the processed chunk.
	 * @param jobId of the job that the chunk is associated.
	 * @param stepContribution represents a contribution to a {@link org.springframework.batch.core.StepExecution}
	 */
	public ChunkResponse(boolean status, int sequence, Long jobId, StepContribution stepContribution) {
		this(status, sequence, jobId, stepContribution, null);
	}

	/**
	 * @param status the result of chunk processing.  True if successful, else it is set to false.
	 * @param sequence associated with the processed chunk.
	 * @param jobId of the job that the chunk is associated.
	 * @param stepContribution represents a contribution to a {@link org.springframework.batch.core.StepExecution}
	 * @param message indicating the state of the response.
	 */
	public ChunkResponse(boolean status, int sequence, Long jobId, StepContribution stepContribution, @Nullable String message) {
		this(status, sequence, jobId, stepContribution, message, false);
	}

	/**
	 * @param input {@link ChunkResponse} containing the state for this instance.
	 * @param redelivered indicates whether the chunk was redelivered for processing.
	 */
	public ChunkResponse(ChunkResponse input, boolean redelivered) {
		this(input.status, input.sequence, input.jobId, input.stepContribution, input.message, redelivered);
	}

	/**
	 * @param status the result of chunk processing.  True if successful, else it is set to false.
	 * @param sequence associated with the processed chunk.
	 * @param jobId of the job that the chunk is associated.
	 * @param stepContribution represents a contribution to a {@link org.springframework.batch.core.StepExecution}
	 * @param message indicating the state of the response.
	 * @param redelivered indicates whether the chunk was redelivered for processing.
	 */
	public ChunkResponse(boolean status, int sequence, Long jobId, StepContribution stepContribution, @Nullable String message, boolean redelivered) {
		this.status = status;
		this.sequence = sequence;
		this.jobId = jobId;
		this.stepContribution = stepContribution;
		this.message = message;
		this.redelivered = redelivered;
	}

	/**
	 * @return the {@link StepContribution} to a {@link org.springframework.batch.core.StepExecution}.
	 */
	public StepContribution getStepContribution() {
		return stepContribution;
	}

	/**
	 * @return The job id for this ChunkResponse.
	 */
	public Long getJobId() {
		return jobId;
	}

	/**
	 * @return The sequence for this ChunkResponse.
	 */
	public int getSequence() {
		return sequence;
	}

	/**
	 * @return true if chunk was processed successfully else false.
	 */
	public boolean isSuccessful() {
		return status;
	}

	/**
	 * @return true if chunk was redelivered.
	 */
	public boolean isRedelivered() {
		return redelivered;
	}

	/**
	 * @return message indicating the state of the response.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName() + ": jobId=" + jobId + ", sequence=" + sequence + ", stepContribution=" + stepContribution
				+ ", successful=" + status;
	}

}
