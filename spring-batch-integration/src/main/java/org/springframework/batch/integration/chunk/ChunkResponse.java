package org.springframework.batch.integration.chunk;

import java.io.Serializable;

import org.springframework.batch.repeat.ExitStatus;

public class ChunkResponse implements Serializable {

	private final int skipCount;
	private final Long jobId;
	private final ExitStatus exitStatus;

	public ChunkResponse(ExitStatus exitStatus, Long jobId, int skipCount) {
		this.exitStatus = exitStatus;
		this.jobId = jobId;
		this.skipCount = skipCount;
	}

	public int getSkipCount() {
		return skipCount;
	}

	public Long getJobId() {
		return jobId;
	}

	public ExitStatus getExitStatus() {
		return exitStatus;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName()+": jobId="+jobId+", skipCount="+skipCount+", status="+exitStatus;
	}

}
