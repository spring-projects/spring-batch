package org.springframework.batch.sample.item.processor;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.sample.dao.OrderWriter;
import org.springframework.batch.sample.domain.Order;
import org.springframework.batch.sample.item.processor.OrderProcessor;

public class OrderProcessorTests extends TestCase {

	private MockControl writerControl;
	private OrderWriter writer;
	private OrderProcessor processor;
	
	public void setUp() {
		
		//create mock writer
		writerControl = MockControl.createControl(OrderWriter.class);
		writer = (OrderWriter)writerControl.getMock();
		
		//create processor
		processor = new OrderProcessor();
		processor.setWriter(writer);
	}
	
	public void testProcess() {
		
		Order order = new Order();
		//set-up mock writer
		writer.write(order);
		writerControl.replay();
		
		//call tested method
		processor.process(order);
		
		//verify method calls
		writerControl.verify();
	}
	
	public void testProcessWithException() {
		
		writerControl.replay();
		//call tested method
		try {
			processor.process(this);
			fail("Batch critical exception was expected");
		} catch (BatchCriticalException bce) {
			assertTrue(true);
		}
		writerControl.verify();
	}
}
