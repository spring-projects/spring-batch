package org.springframework.batch.item.support;

import java.util.ArrayList;
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
	private CompositeItemWriter<Object> itemProcessor = new CompositeItemWriter<Object>();
	
	/**
	 * Regular usage scenario.
	 * All injected processors should be called.
	 */
	public void testProcess() throws Exception {
		
		final int NUMBER_OF_WRITERS = 10;
		Object data = new Object();
		
		List<MockControl> controls = new ArrayList<MockControl>(NUMBER_OF_WRITERS);
		List<ItemWriter<Object>> writers = new ArrayList<ItemWriter<Object>>(NUMBER_OF_WRITERS);
		
		for (int i = 0; i < NUMBER_OF_WRITERS; i++) {
			MockControl control = MockControl.createStrictControl(ItemWriter.class);
			ItemWriter writer = (ItemWriter) control.getMock();
			
			writer.write(data);
			control.setVoidCallable();
			control.replay();
			
			writers.add(writer);
			controls.add(control);
		}
		
		itemProcessor.setDelegates(writers);
		itemProcessor.write(data);
		
		for (MockControl control : controls) {
			control.verify();
		}
	}
	
}
