/*
 * Copyright 2006-2023 the original author or authors.
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

package org.springframework.batch.integration.chunk;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.core.step.item.ChunkProcessor;
import org.springframework.batch.core.step.item.FaultTolerantChunkProcessor;
import org.springframework.batch.core.step.skip.NonSkippableReadException;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipListenerFailedException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.retry.RetryException;
import org.springframework.util.Assert;

/**
 * A {@link ChunkHandler} based on a {@link ChunkProcessor}. Knows how to distinguish
 * between a processor that is fault tolerant, and one that is not. If the processor is
 * fault tolerant then exceptions can be propagated on the assumption that there will be a
 * roll back and the request will be re-delivered.
 *
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @param <S> the type of the items in the chunk to be handled
 */
@MessageEndpoint
public class ChunkProcessorChunkHandler<S> implements ChunkHandler<S>, InitializingBean {

	private static final Log logger = LogFactory.getLog(ChunkProcessorChunkHandler.class);

	private ChunkProcessor<S> chunkProcessor;

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(chunkProcessor != null, "A ChunkProcessor must be provided");
	}

	/**
	 * Public setter for the {@link ChunkProcessor}.
	 * @param chunkProcessor the chunkProcessor to set
	 */
	public void setChunkProcessor(ChunkProcessor<S> chunkProcessor) {
		this.chunkProcessor = chunkProcessor;
	}

	/**
	 *
	 * @see ChunkHandler#handleChunk(ChunkRequest)
	 */
	@Override
	@ServiceActivator
	public ChunkResponse handleChunk(ChunkRequest<S> chunkRequest) throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("Handling chunk: " + chunkRequest);
		}

		StepContribution stepContribution = chunkRequest.getStepContribution();

		Throwable failure = process(chunkRequest, stepContribution);
		if (failure != null) {
			logger.debug("Failed chunk", failure);
			return new ChunkResponse(false, chunkRequest.getSequence(), chunkRequest.getJobId(), stepContribution,
					failure.getClass().getName() + ": " + failure.getMessage());
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Completed chunk handling with " + stepContribution);
		}
		return new ChunkResponse(true, chunkRequest.getSequence(), chunkRequest.getJobId(), stepContribution);

	}

	/**
	 * @param chunkRequest the current request
	 * @param stepContribution the step contribution to update
	 * @throws Exception if there is a fatal exception
	 */
	@SuppressWarnings(value = { "unchecked", "rawtypes" })
	private Throwable process(ChunkRequest<S> chunkRequest, StepContribution stepContribution) throws Exception {

		Chunk chunk = chunkRequest.getItems();
		Throwable failure = null;
		try {
			chunkProcessor.process(stepContribution, chunk);
		}
		catch (SkipLimitExceededException | NonSkippableReadException | SkipListenerFailedException | RetryException
				| JobInterruptedException e) {
			failure = e;
		}
		catch (Exception e) {
			if (chunkProcessor instanceof FaultTolerantChunkProcessor<?, ?>) {
				// try again...
				throw e;
			}
			else {
				failure = e;
			}
		}

		return failure;

	}

}
