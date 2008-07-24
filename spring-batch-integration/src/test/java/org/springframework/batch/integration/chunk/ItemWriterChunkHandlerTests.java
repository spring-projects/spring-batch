package org.springframework.batch.integration.chunk;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.listener.SkipListenerSupport;
import org.springframework.batch.core.step.skip.AlwaysSkipItemSkipPolicy;
import org.springframework.batch.integration.chunk.ChunkRequest;
import org.springframework.batch.integration.chunk.ChunkResponse;
import org.springframework.batch.integration.chunk.ItemWriterChunkHandler;
import org.springframework.batch.item.support.AbstractItemWriter;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.util.StringUtils;

public class ItemWriterChunkHandlerTests {

	private ItemWriterChunkHandler<Object> handler = new ItemWriterChunkHandler<Object>();

	protected int count = 0;

	private SkipListenerSupport listener = new SkipListenerSupport() {
		@Override
		public void onSkipInWrite(Object item, Throwable t) {
			count++;
		}
	};

	@SuppressWarnings("unchecked")
	@Test
	public void testVanillaHandleChunk() {
		handler.setItemWriter(new AbstractItemWriter() {
			public void write(Object item) throws Exception {
				count++;
			}
		});
		ChunkResponse response = handler.handleChunk(new ChunkRequest(StringUtils.commaDelimitedListToSet("foo,bar"),
				12L, 10));
		assertEquals(0, response.getSkipCount());
		assertEquals(new Long(12L), response.getJobId());
		assertEquals(ExitStatus.CONTINUABLE, response.getExitStatus());
		assertEquals(2, count);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSetItemSkipPolicy() {
		handler.setItemWriter(new AbstractItemWriter() {
			public void write(Object item) throws Exception {
				count++;
				throw new RuntimeException("Planned failure");
			}
		});
		handler.setItemSkipPolicy(new AlwaysSkipItemSkipPolicy());
		ChunkResponse response = handler.handleChunk(new ChunkRequest(StringUtils.commaDelimitedListToSet("foo,bar"),
				12L, 10));
		assertEquals(2, response.getSkipCount());
		assertEquals(new Long(12L), response.getJobId());
		assertEquals(ExitStatus.CONTINUABLE, response.getExitStatus());
		assertEquals(2, count);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testRegisterSkipListener() {
		handler.setItemWriter(new AbstractItemWriter() {
			public void write(Object item) throws Exception {
				count++;
				throw new RuntimeException("Planned failure");
			}
		});
		handler.setItemSkipPolicy(new AlwaysSkipItemSkipPolicy());
		handler.registerSkipListener(listener);
		ChunkResponse response = handler.handleChunk(new ChunkRequest(StringUtils.commaDelimitedListToSet("foo,bar"),
				12L, 10));
		assertEquals(2, response.getSkipCount());
		assertEquals(4, count);
	}

	@Test
	public void testSetSkipListeners() {
		handler.setSkipListeners(new SkipListener[] { listener });
		testRegisterSkipListener();
	}

}
