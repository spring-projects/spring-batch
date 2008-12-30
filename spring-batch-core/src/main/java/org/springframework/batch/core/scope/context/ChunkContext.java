package org.springframework.batch.core.scope.context;

import java.util.Arrays;

import org.springframework.core.AttributeAccessorSupport;

/**
 * @author Dave Syer
 * 
 */
public class ChunkContext extends AttributeAccessorSupport {

	private final StepContext stepContext;
	private boolean complete = false;

	/**
	 * @param stepContext the current step context
	 */
	public ChunkContext(StepContext stepContext) {
		this.stepContext = stepContext;
	}

	/**
	 * @return the current step context
	 */
	public StepContext getStepContext() {
		return stepContext;
	}

	/**
	 * @return true if there is no more processing to be done on this chunk
	 */
	public boolean isComplete() {
		return complete;
	}
	
	/**
	 * Setter for the flag to signal complete processing of a chunk.
	 */
	public void setComplete() {
		this.complete = true;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("ChunkContext: attributes=%s, complete=%b, stepContext=%s", Arrays.asList(attributeNames()), complete, stepContext);
	}

}