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

package org.springframework.batch.integration.chunk;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.item.ChunkProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.util.Assert;

@MessageEndpoint
public class ChunkProcessorChunkHandler<S> implements ChunkHandler<S>, InitializingBean {

	private static final Log logger = LogFactory.getLog(ChunkProcessorChunkHandler.class);

	private ChunkProcessor<S> chunkProcessor;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(chunkProcessor, "A ChunkProcessor must be provided");
	}

	/**
	 * Public setter for the {@link ChunkProcessor}.
	 * 
	 * @param chunkProcessor the chunkProcessor to set
	 */
	public void setChunkProcessor(ChunkProcessor<S> chunkProcessor) {
		this.chunkProcessor = chunkProcessor;
	}

	/**
	 * 
	 * @see ChunkHandler#handleChunk(ChunkRequest)
	 */
	@ServiceActivator
	public ChunkResponse handleChunk(ChunkRequest<S> chunkRequest) {

		logger.debug("Handling chunk: " + chunkRequest);

		StepContribution stepContribution = chunkRequest.getStepContribution();
		try {
			chunkProcessor.process(stepContribution, chunkRequest.getChunk());
		}
		catch (Exception e) {
			logger.debug("Failed chunk", e);
			return new ChunkResponse(false, chunkRequest.getJobId(), stepContribution, e.getClass().getName() + ": "
					+ e.getMessage());
		}

		logger.debug("Completed chunk handling with " + stepContribution);
		return new ChunkResponse(true, chunkRequest.getJobId(), stepContribution);

	}
}
