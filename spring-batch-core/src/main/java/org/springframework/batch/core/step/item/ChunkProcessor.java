package org.springframework.batch.core.step.item;

import org.springframework.batch.core.StepContribution;

public interface ChunkProcessor<I> {
	
	void process(StepContribution contribution, Chunk<I> chunk) throws Exception;

}
