package org.springframework.batch.sample.item.processor;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.sample.dao.TradeWriter;
import org.springframework.batch.sample.domain.Trade;
import org.springframework.batch.sample.item.processor.TradeProcessor;

public class TradeProcessorTests extends TestCase {

	private MockControl writerControl;
	private TradeWriter writer;
	private TradeProcessor processor;
	
	public void setUp() {
		
		//create mock writer
		writerControl = MockControl.createControl(TradeWriter.class);
		writer = (TradeWriter)writerControl.getMock();
		
		//create processor
		processor = new TradeProcessor();
		processor.setWriter(writer);
	}
		
	public void testProcess() {
		
		Trade trade = new Trade();
		//set-up mock writer
		writer.writeTrade(trade);
		writerControl.replay();
		
		//call tested method
		processor.process(trade);
		
		//verify method calls
		writerControl.verify();
	}
	
	public void testProcessNonTradeObject() {
		
		writerControl.replay();
		//call tested method
		processor.process(this);
		writerControl.verify();
	}
}
