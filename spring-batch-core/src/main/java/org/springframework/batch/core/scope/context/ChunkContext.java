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

package org.springframework.batch.core.scope.context;

import java.util.Arrays;

import org.springframework.core.AttributeAccessorSupport;

/**
 * Context object for weakly typed data stored for the duration of a chunk
 * (usually a group of items processed together in a transaction). If there is a
 * rollback and the chunk is retried the same context will be associated with
 * it.
 * 
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("ChunkContext: attributes=%s, complete=%b, stepContext=%s", Arrays
				.asList(attributeNames()), complete, stepContext);
	}

}