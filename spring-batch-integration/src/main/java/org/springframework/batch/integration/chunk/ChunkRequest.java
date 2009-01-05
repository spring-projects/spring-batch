package org.springframework.batch.integration.chunk;

import java.io.Serializable;
import java.util.Collection;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.item.Chunk;

public class ChunkRequest<T> implements Serializable {

	private final Long jobId;
	private final Chunk<T> items;
	private final StepContribution stepContribution;

	public ChunkRequest(Collection<? extends T> items, Long jobId, StepContribution stepContribution) {
		this.items = new Chunk<T>(items);
		this.jobId = jobId;
		this.stepContribution = stepContribution;
	}

	public Long getJobId() {
		return jobId;
	}

	public Chunk<T> getChunk() {
		return items;
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
		return getClass().getSimpleName()+": jobId="+jobId+", contribution="+stepContribution+", item count="+items.size();
	}

}
