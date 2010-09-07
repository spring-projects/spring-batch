package org.springframework.batch.item.support;

import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

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
			ItemWriter<? super Object> writer = createStrictMock(ItemWriter.class);

			writer.write(data);
			expectLastCall().once();
			replay(writer);

			writers.add(writer);
		}

		itemWriter.setDelegates(writers);
		itemWriter.write(data);

		for (ItemWriter<? super Object> writer : writers) {
			verify(writer);
		}
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
		ItemStreamWriter<? super Object> writer = createStrictMock(ItemStreamWriter.class);
		List<Object> data = Collections.singletonList(new Object());
		ExecutionContext executionContext = new ExecutionContext();
		if (expectOpen) {
			writer.open(executionContext);
			expectLastCall().once();
		}
		writer.write(data);
		expectLastCall().once();
		replay(writer);

		List<ItemWriter<? super Object>> writers = new ArrayList<ItemWriter<? super Object>>();
		writers.add(writer);

		itemWriter.setDelegates(writers);
		if (expectOpen) {
			itemWriter.open(executionContext);
		}
		itemWriter.write(data);

		verify(writer);
	}

}
