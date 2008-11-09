package org.springframework.batch.integration.chunk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Test;
import org.springframework.util.StringUtils;

public class ChunkProcessorChunkHandlerTests {

	private ChunkProcessorChunkHandler<Object> handler = new ChunkProcessorChunkHandler<Object>();

	protected int count = 0;

	@Test
	public void testVanillaHandleChunk() {
		handler.setChunkProcessor(new ChunkProcessor<Object>() {
			public int process(Collection<? extends Object> items, int skipCount) throws Exception {
				count+=items.size();
				return 0;
			}
		});
		ChunkResponse response = handler.handleChunk(new ChunkRequest<Object>(StringUtils.commaDelimitedListToSet("foo,bar"),
				12L, 10));
		assertEquals(0, response.getSkipCount());
		assertEquals(new Long(12L), response.getJobId());
		assertTrue(response.isSuccessful());
		assertEquals(2, count);
	}

}
