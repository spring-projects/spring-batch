package org.springframework.batch.jsr.item;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.batch.api.chunk.ItemWriter;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;

public class ItemWriterAdapterTests {

	private ItemWriterAdapter adapter;
	@Mock
	private ItemWriter delegate;
	@Mock
	private ExecutionContext executionContext;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		adapter = new ItemWriterAdapter(delegate);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateWithNull() {
		adapter = new ItemWriterAdapter(null);
	}

	@Test
	public void testOpen() throws Exception {
		when(executionContext.get("ItemWriter.writer.checkpoint")).thenReturn("checkpoint");

		adapter.open(executionContext);

		verify(delegate).open("checkpoint");
	}

	@Test(expected=ItemStreamException.class)
	public void testOpenException() throws Exception {
		when(executionContext.get("ItemWriter.writer.checkpoint")).thenReturn("checkpoint");

		doThrow(new Exception("expected")).when(delegate).open("checkpoint");

		adapter.open(executionContext);
	}

	@Test
	public void testUpdate() throws Exception {
		when(delegate.checkpointInfo()).thenReturn("checkpoint");

		adapter.update(executionContext);

		verify(executionContext).put("ItemWriter.writer.checkpoint", "checkpoint");
	}

	@Test(expected=ItemStreamException.class)
	public void testUpdateException() throws Exception {
		doThrow(new Exception("expected")).when(delegate).checkpointInfo();

		adapter.update(executionContext);
	}

	@Test
	public void testClose() throws Exception {
		adapter.close();

		verify(delegate).close();
	}

	@Test(expected=ItemStreamException.class)
	public void testCloseException() throws Exception {
		doThrow(new Exception("expected")).when(delegate).close();

		adapter.close();
	}

	@Test
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void testWrite() throws Exception {
		List items = new ArrayList();

		items.add("item1");
		items.add("item2");

		adapter.write(items);

		verify(delegate).writeItems(items);
	}
}
