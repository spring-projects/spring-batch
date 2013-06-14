package org.springframework.batch.core.jsr;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import javax.batch.api.chunk.listener.ItemReadListener;
import javax.batch.operations.BatchRuntimeException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ItemReadListenerAdapterTests {

	private ItemReadListenerAdapter<String> adapter;
	@Mock
	private ItemReadListener delegate;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		adapter = new ItemReadListenerAdapter<String>(delegate);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNullDelegate() {
		adapter = new ItemReadListenerAdapter<String>(null);
	}

	@Test
	public void testBeforeRead() throws Exception {
		adapter.beforeRead();

		verify(delegate).beforeRead();
	}

	@Test(expected=BatchRuntimeException.class)
	public void testBeforeReadException() throws Exception {
		doThrow(new Exception("Should occur")).when(delegate).beforeRead();

		adapter.beforeRead();
	}

	@Test
	public void testAfterRead() throws Exception {
		String item = "item";

		adapter.afterRead(item);

		verify(delegate).afterRead(item);
	}

	@Test(expected=BatchRuntimeException.class)
	public void testAfterReadException() throws Exception {
		String item = "item";
		Exception expected = new Exception("expected");

		doThrow(expected).when(delegate).afterRead(item);

		adapter.afterRead(item);
	}

	@Test
	public void testOnReadError() throws Exception {
		Exception cause = new Exception ("cause");

		adapter.onReadError(cause);

		verify(delegate).onReadError(cause);
	}

	@Test(expected=BatchRuntimeException.class)
	public void testOnReadErrorException() throws Exception {
		Exception cause = new Exception ("cause");
		Exception result = new Exception("result");

		doThrow(result).when(delegate).onReadError(cause);

		adapter.onReadError(cause);
	}
}
