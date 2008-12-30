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
