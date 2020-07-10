/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.repeat.policy;

import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.context.RepeatContextSupport;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.util.ClassUtils;

/**
 * Policy for terminating a batch after a fixed number of operations. Internal
 * state is maintained and a counter incremented, so successful use of this
 * policy requires that isComplete() is only called once per batch item. Using
 * the standard {@link RepeatTemplate} should ensure this contract is kept, but it needs
 * to be carefully monitored.
 * 
 * @author Dave Syer
 * 
 */
public class SimpleCompletionPolicy extends DefaultResultCompletionPolicy {

	public static final int DEFAULT_CHUNK_SIZE = 5;

	int chunkSize = 0;

	public SimpleCompletionPolicy() {
		this(DEFAULT_CHUNK_SIZE);
	}

	public SimpleCompletionPolicy(int chunkSize) {
		super();
		this.chunkSize = chunkSize;
	}

	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}

	public int getChunkSize() {
		return chunkSize;
	}

	/**
	 * Reset the counter.
	 * 
	 * @see org.springframework.batch.repeat.CompletionPolicy#start(RepeatContext)
	 */
    @Override
	public RepeatContext start(RepeatContext context) {
		return new SimpleTerminationContext(context);
	}

	/**
	 * Terminate if the chunk size has been reached, or the result is null.
	 * 
	 * @see org.springframework.batch.repeat.CompletionPolicy#isComplete(RepeatContext,
	 * RepeatStatus)
	 * @throws RuntimeException (normally terminating the batch) if the result is
	 * itself an exception.
	 */
    @Override
	public boolean isComplete(RepeatContext context, RepeatStatus result) {
		return super.isComplete(context, result) || ((SimpleTerminationContext) context).isComplete();
	}

	/**
	 * Terminate if the chunk size has been reached.
	 * 
	 * @see org.springframework.batch.repeat.CompletionPolicy#isComplete(RepeatContext)
	 */
    @Override
	public boolean isComplete(RepeatContext context) {
		return ((SimpleTerminationContext) context).isComplete();
	}

	/**
	 * Increment the counter in the context.
	 * 
	 * @see org.springframework.batch.repeat.CompletionPolicy#update(RepeatContext)
	 */
    @Override
	public void update(RepeatContext context) {
		((SimpleTerminationContext) context).update();
	}

	protected class SimpleTerminationContext extends RepeatContextSupport {

		public SimpleTerminationContext(RepeatContext context) {
			super(context);
		}

		public void update() {
			increment();
		}

		public boolean isComplete() {
			return getStartedCount() >= chunkSize;
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
    @Override
	public String toString() {
		return ClassUtils.getShortName(SimpleCompletionPolicy.class)+": chunkSize="+chunkSize;
	}

}
