package org.springframework.batch.integration.chunk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.item.Chunk;
import org.springframework.batch.core.step.item.ChunkProcessor;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.util.StringUtils;

public class ChunkProcessorChunkHandlerTests {

	private ChunkProcessorChunkHandler<Object> handler = new ChunkProcessorChunkHandler<Object>();

	protected int count = 0;

	@Test
	public void testVanillaHandleChunk() throws Exception {
		handler.setChunkProcessor(new ChunkProcessor<Object>() {
			public void process(StepContribution contribution, Chunk<Object> chunk) throws Exception {
				count += chunk.size();
			}
		});
		StepContribution stepContribution = MetaDataInstanceFactory.createStepExecution().createStepContribution();
		ChunkResponse response = handler.handleChunk(new ChunkRequest<Object>(0, StringUtils
						.commaDelimitedListToSet("foo,bar"), 12L, stepContribution));
		assertEquals(stepContribution, response.getStepContribution());
		assertEquals(12, response.getJobId().longValue());
		assertTrue(response.isSuccessful());
		assertEquals(2, count);
	}

}
