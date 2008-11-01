package org.springframework.batch.sample.item.writer;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.sample.domain.Order;
import org.springframework.batch.sample.item.writer.OrderWriter;

public class OrderWriterTests extends TestCase {

	private MockControl writerControl;
	private OrderWriter processor;
	private ItemWriter writer;
	
	public void setUp() {
		
		//create mock writer
		writerControl = MockControl.createControl(ItemWriter.class);
		writer = (ItemWriter)writerControl.getMock();
		
		//create processor
		processor = new OrderWriter();
		processor.setDelegate(writer);
	}
	
	public void testProcess() throws Exception {
		
		Order order = new Order();
		//set-up mock writer
		writer.write(order);
		writerControl.replay();
		
		//call tested method
		processor.write(order);
		
		//verify method calls
		writerControl.verify();
	}
	
	public void testProcessWithException() throws Exception {
		
		writerControl.replay();
		//call tested method
		try {
			processor.write(this);
			fail("Batch critical exception was expected");
		} catch (UnexpectedJobExecutionException bce) {
			assertTrue(true);
		}
		writerControl.verify();
	}
}
