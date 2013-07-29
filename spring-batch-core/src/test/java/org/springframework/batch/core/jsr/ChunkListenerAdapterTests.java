package org.springframework.batch.core.jsr;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.batch.api.chunk.listener.ChunkListener;
import javax.batch.operations.BatchRuntimeException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.scope.context.ChunkContext;

public class ChunkListenerAdapterTests {

	private ChunkListenerAdapter adapter;
	@Mock
	private ChunkListener delegate;
	@Mock
	private ChunkContext context;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		adapter = new ChunkListenerAdapter(delegate);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNullDelegate() {
		adapter = new ChunkListenerAdapter(null);
	}

	@Test
	public void testBeforeChunk() throws Exception {
		adapter.beforeChunk(null);

		verify(delegate).beforeChunk();
	}

	@Test(expected=BatchRuntimeException.class)
	public void testBeforeChunkException() throws Exception {
		doThrow(new Exception("This is expected")).when(delegate).beforeChunk();
		adapter.beforeChunk(null);
	}

	@Test
	public void testAfterChunk() throws Exception {
		adapter.afterChunk(null);

		verify(delegate).afterChunk();
	}

	@Test(expected=BatchRuntimeException.class)
	public void testAfterChunkException() throws Exception {
		doThrow(new Exception("This is expected")).when(delegate).afterChunk();
		adapter.afterChunk(null);
	}

	@Test(expected=BatchRuntimeException.class)
	public void testAfterChunkErrorNullContext() throws Exception {
		adapter.afterChunkError(null);
	}

	@Test(expected=BatchRuntimeException.class)
	public void testAfterChunkErrorException() throws Exception {
		doThrow(new Exception("This is expected")).when(delegate).afterChunk();
		adapter.afterChunk(null);
	}

	@Test
	public void testAfterChunkError() throws Exception {
		Exception exception = new Exception("This was expected");

		when(context.getAttribute(org.springframework.batch.core.ChunkListener.ROLLBACK_EXCEPTION_KEY)).thenReturn(exception);

		adapter.afterChunkError(context);

		verify(delegate).onError(exception);
	}
}
