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

	public ChunkResponse(Long jobId, StepContribution stepContribution) {
		this(true, jobId, stepContribution, null);
	}

	public ChunkResponse(boolean status, Long jobId, StepContribution stepContribution) {
		this(status, jobId, stepContribution, null);
	}

	public ChunkResponse(boolean status, Long jobId, StepContribution stepContribution, String message) {
		this.status = status;
		this.jobId = jobId;
		this.stepContribution = stepContribution;
		this.message = message;
	}

	public StepContribution getStepContribution() {
		return stepContribution;
	}

	public Long getJobId() {
		return jobId;
	}

	public boolean isSuccessful() {
		return status;
	}

	public String getMessage() {
		return message;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName() + ": jobId=" + jobId + ", stepContribution=" + stepContribution
				+ ", successful=" + status;
	}

}
