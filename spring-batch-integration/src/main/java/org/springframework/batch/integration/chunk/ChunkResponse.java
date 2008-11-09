package org.springframework.batch.integration.chunk;

import java.io.Serializable;

public class ChunkResponse implements Serializable {

	private final int skipCount;
	private final Long jobId;
	private final boolean status;
	private final String message;

	public ChunkResponse(Long jobId) {
		this(true, jobId, 0, null);
	}

	public ChunkResponse(Long jobId, int skipCount) {
		this(true, jobId, skipCount, null);
	}

	public ChunkResponse(boolean status, Long jobId, int skipCount) {
		this(status, jobId, skipCount, null);
	}

	public ChunkResponse(boolean status, Long jobId, int skipCount, String message) {
		this.status = status;
		this.jobId = jobId;
		this.skipCount = skipCount;
		this.message = message;
	}

	public int getSkipCount() {
		return skipCount;
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
		return getClass().getSimpleName()+": jobId="+jobId+", skipCount="+skipCount+", successful="+status;
	}

}
