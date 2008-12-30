package org.springframework.batch.integration.chunk;

import java.io.Serializable;

import org.springframework.batch.core.StepContribution;

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
		return getClass().getSimpleName()+": jobId="+jobId+", stepContribution="+stepContribution+", successful="+status;
	}

}
