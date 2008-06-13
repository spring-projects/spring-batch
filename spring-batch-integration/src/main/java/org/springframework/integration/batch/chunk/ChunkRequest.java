package org.springframework.integration.batch.chunk;

import java.io.Serializable;
import java.util.Collection;

public class ChunkRequest implements Serializable {

	private final int skipCount;
	private final Long jobId;
	private final Collection<Object> items;

	public ChunkRequest(Collection<Object> items, Long jobId, int skipCount) {
		this.items = items;
		this.jobId = jobId;
		this.skipCount = skipCount;
	}

	public int getSkipCount() {
		return skipCount;
	}

	public Long getJobId() {
		return jobId;
	}

	public Collection<Object> getItems() {
		return items;
	}

}
