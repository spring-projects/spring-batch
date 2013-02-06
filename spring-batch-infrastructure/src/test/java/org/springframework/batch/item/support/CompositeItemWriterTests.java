package org.springframework.batch.item.support;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.batch.item.ItemWriter;

/**
 * Tests for {@link CompositeItemWriter}
 * 
 * @author Robert Kasanicky
 * @author Will Schipp
 */
public class CompositeItemWriterTests {

	// object under test
	private CompositeItemWriter<Object> itemWriter = new CompositeItemWriter<Object>();

	/**
	 * Regular usage scenario. All injected processors should be called.
	 */
	@Test
	public void testProcess() throws Exception {

		final int NUMBER_OF_WRITERS = 10;
		List<Object> data = Collections.singletonList(new Object());

		List<ItemWriter<? super Object>> writers = new ArrayList<ItemWriter<? super Object>>();

		for (int i = 0; i < NUMBER_OF_WRITERS; i++) {
			@SuppressWarnings("unchecked")
			ItemWriter<? super Object> writer = mock(ItemWriter.class);

			writer.write(data);

			writers.add(writer);
		}

		itemWriter.setDelegates(writers);
		itemWriter.write(data);


	}

	@Test
	public void testItemStreamCalled() throws Exception {
		doTestItemStream(true);
	}

	@Test
	public void testItemStreamNotCalled() throws Exception {
		doTestItemStream(false);
	}

	private void doTestItemStream(boolean expectOpen) throws Exception {
		@SuppressWarnings("unchecked")
		ItemStreamWriter<? super Object> writer = mock(ItemStreamWriter.class);
		List<Object> data = Collections.singletonList(new Object());
		ExecutionContext executionContext = new ExecutionContext();
		if (expectOpen) {
			writer.open(executionContext);
		}
		writer.write(data);

		List<ItemWriter<? super Object>> writers = new ArrayList<ItemWriter<? super Object>>();
		writers.add(writer);

		itemWriter.setDelegates(writers);
		if (expectOpen) {
			itemWriter.open(executionContext);
		}
		itemWriter.write(data);
	}

}
