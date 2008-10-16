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
package org.springframework.batch.core.scope;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;

/**
 * Convenient base class for clients who need to do something in a repeat
 * callback inside a {@link Step}.
 * 
 * @author Dave Syer
 * 
 */
public abstract class StepContextRepeatCallback implements RepeatCallback {

	private final Queue<ChunkContext> attributeQueue = new LinkedBlockingQueue<ChunkContext>();

	private final StepContext stepContext;

	/**
	 * @param stepExecution
	 */
	public StepContextRepeatCallback(StepExecution stepExecution) {
		this.stepContext = new StepContext(stepExecution);
	}

	/**
	 * Manage the {@link StepContext} lifecycle to ensure that the current
	 * thread has a reference to the context, even if the callback is executed
	 * in a pooled thread. Handles the registration and de-registration of the
	 * step context, so clients should not duplicate those calls.
	 * 
	 * @see RepeatCallback#doInIteration(RepeatContext)
	 */
	public ExitStatus doInIteration(RepeatContext context) throws Exception {

		ChunkContext chunkContext = attributeQueue.poll();
		if (chunkContext == null) {
			chunkContext = new ChunkContext();
		}
		int start = stepContext.attributeNames().length;

		// The StepContext has to be the same for all chunks,
		// otherwise step-scoped beans will be re-initialised for each chunk.
		StepSynchronizationManager.register(stepContext);
		try {
			return doInStepContext(context, stepContext);
		}
		finally {
			// Still some stuff to do with the data in this chunk,
			// pass it back
			if (chunkContext.attributeNames().length > start) {
				attributeQueue.add(chunkContext);
			}
			StepSynchronizationManager.close();
		}
	}

	/**
	 * Do the work required for this portion of the step. The
	 * {@link StepContext} provided is managed by the base class, so that if
	 * there is still work to do for the task in hand state can be stored here.
	 * In a multi-threaded client, the base class ensures that only one thread
	 * at a time can be working on each instance of {@link StepContext}. Workers
	 * should signal that they are finished with a context by removing all the
	 * attributes they have added. If a worker does not remove them another
	 * thread might see stale state.
	 * 
	 * @param context the current {@link RepeatContext}
	 * @param stepContext the step context in which to carry out the work
	 * @return the exit status from the execution
	 * @throws Exception implementations can throw an exception if anything goes
	 * wrong
	 */
	public abstract ExitStatus doInStepContext(RepeatContext context, StepContext stepContext) throws Exception;

}
