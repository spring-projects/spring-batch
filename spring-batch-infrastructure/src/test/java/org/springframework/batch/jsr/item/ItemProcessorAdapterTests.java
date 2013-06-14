package org.springframework.batch.jsr.item;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import javax.batch.api.chunk.ItemProcessor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ItemProcessorAdapterTests {

	private ItemProcessorAdapter adapter;
	@Mock
	private ItemProcessor delegate;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		adapter = new ItemProcessorAdapter(delegate);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateWithNull() {
		adapter = new ItemProcessorAdapter(null);
	}

	@Test
	public void testProcess() throws Exception {
		String input = "input";
		String output = "output";

		when(delegate.processItem(input)).thenReturn(output);

		assertEquals(output, adapter.process(input));
	}
}
