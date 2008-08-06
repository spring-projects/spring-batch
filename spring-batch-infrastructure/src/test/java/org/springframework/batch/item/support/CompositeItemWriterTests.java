package org.springframework.batch.item.support;

import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import junit.framework.TestCase;

import org.springframework.batch.item.ItemWriter;

/**
 * Tests for {@link CompositeItemWriter}
 * 
 * @author Robert Kasanicky
 */
public class CompositeItemWriterTests extends TestCase {

	// object under test
	private CompositeItemWriter<Object> itemProcessor = new CompositeItemWriter<Object>();
	
	/**
	 * Regular usage scenario.
	 * All injected processors should be called.
	 */
	
	public void testProcess() throws Exception {
		
		final int NUMBER_OF_WRITERS = 10;
		Object data = new Object();
		
		@SuppressWarnings("unchecked")
		ItemWriter<Object>[] writers = new ItemWriter[NUMBER_OF_WRITERS];
		
		for (int i = 0; i < NUMBER_OF_WRITERS; i++) {
			@SuppressWarnings("unchecked")
			ItemWriter<Object> writer = createStrictMock(ItemWriter.class);
			
			writer.write(data);
			expectLastCall().once();
			replay(writer);
			
			writers[i] = writer;
		}
		
		itemProcessor.setDelegates(writers);
		itemProcessor.write(data);
		
		for (ItemWriter<Object> writer : writers) {
			verify(writer);
		}
	}
	
}
