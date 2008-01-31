package org.springframework.batch.item.writer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.item.ItemWriter;

/**
 * Tests for {@link CompositeItemWriter}
 * 
 * @author Robert Kasanicky
 */
public class CompositeItemWriterTests extends TestCase {

	// object under test
	private CompositeItemWriter itemProcessor = new CompositeItemWriter();
	
	/**
	 * Regular usage scenario.
	 * All injected processors should be called.
	 */
	public void testProcess() throws Exception {
		
		final int NUMBER_OF_PROCESSORS = 10;
		Object data = new Object();
		
		List controls = new ArrayList(NUMBER_OF_PROCESSORS);
		List processors = new ArrayList(NUMBER_OF_PROCESSORS);
		
		for (int i = 0; i < NUMBER_OF_PROCESSORS; i++) {
			MockControl control = MockControl.createStrictControl(ItemWriter.class);
			ItemWriter processor = (ItemWriter) control.getMock();
			
			processor.write(data);
			control.setVoidCallable();
			control.replay();
			
			processors.add(processor);
			controls.add(control);
		}
		
		itemProcessor.setDelegates(processors);
		itemProcessor.write(data);
		
		for (Iterator iterator = controls.iterator(); iterator.hasNext();) {
			MockControl control = (MockControl) iterator.next();
			control.verify();
		}
	}
		
	public void testClose() throws Exception {
		
		final int NUMBER_OF_PROCESSORS = 10;
		
		List processors = new ArrayList(NUMBER_OF_PROCESSORS);
		final List list = new ArrayList(NUMBER_OF_PROCESSORS);
		
		for (int i = 0; i < NUMBER_OF_PROCESSORS; i++) {
			ItemWriter processor = new AbstractItemWriter() {
				public void write(Object item) throws Exception {
					throw new IllegalStateException("No way!");
				}
				public void close() throws Exception {
					list.add(this);
				}
			};
			processor.close();
			processors.add(processor);
		}
		
		itemProcessor.setDelegates(processors);
		itemProcessor.close();
		
		assertEquals(NUMBER_OF_PROCESSORS, list.size());
		
	}
	
}
