package org.springframework.batch.integration.chunk;

import java.io.Serializable;

import org.springframework.batch.core.BatchStatus;

public class ChunkResponse implements Serializable {

	private final int skipCount;
	private final Long jobId;
	private final BatchStatus status;

	public ChunkResponse(BatchStatus status, Long jobId, int skipCount) {
		this.status = status;
		this.jobId = jobId;
		this.skipCount = skipCount;
	}

	public int getSkipCount() {
		return skipCount;
	}

	public Long getJobId() {
		return jobId;
	}

	public BatchStatus getStatus() {
		return status;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName()+": jobId="+jobId+", skipCount="+skipCount+", status="+status;
	}

}
