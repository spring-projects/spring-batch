package org.springframework.batch.core.step.item;

import org.springframework.batch.core.StepContribution;

public interface ChunkProvider<T> {

	Chunk<T> provide(StepContribution contribution) throws Exception;
	
	void postProcess(StepContribution contribution, Chunk<T> chunk);
	
}
