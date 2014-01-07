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

package org.springframework.batch.integration.chunk;

import java.io.Serializable;

import org.springframework.batch.core.StepContribution;

/**
 * Encapsulates a response to processing a chunk of items, summarising the result as a {@link StepContribution}.
 * 
 * @author Dave Syer
 * 
 */
public class ChunkResponse implements Serializable {

	private final StepContribution stepContribution;

	private final Long jobId;

	private final boolean status;

	private final String message;
	
	private final boolean redelivered;

	private final int sequence;

	public ChunkResponse(int sequence, Long jobId, StepContribution stepContribution) {
		this(true, sequence, jobId, stepContribution, null);
	}

	public ChunkResponse(boolean status, int sequence, Long jobId, StepContribution stepContribution) {
		this(status, sequence, jobId, stepContribution, null);
	}

	public ChunkResponse(boolean status, int sequence, Long jobId, StepContribution stepContribution, String message) {
		this(status, sequence, jobId, stepContribution, message, false);
	}

	public ChunkResponse(ChunkResponse input, boolean redelivered) {
		this(input.status, input.sequence, input.jobId, input.stepContribution, input.message, redelivered);
	}

	public ChunkResponse(boolean status, int sequence, Long jobId, StepContribution stepContribution, String message, boolean redelivered) {
		this.status = status;
		this.sequence = sequence;
		this.jobId = jobId;
		this.stepContribution = stepContribution;
		this.message = message;
		this.redelivered = redelivered;
	}

	public StepContribution getStepContribution() {
		return stepContribution;
	}

	public Long getJobId() {
		return jobId;
	}
	
	public int getSequence() {
		return sequence;
	}

	public boolean isSuccessful() {
		return status;
	}
	
	public boolean isRedelivered() {
		return redelivered;
	}

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
