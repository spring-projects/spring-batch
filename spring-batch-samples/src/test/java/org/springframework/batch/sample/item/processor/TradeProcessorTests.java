package org.springframework.batch.sample.item.processor;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.sample.dao.TradeDao;
import org.springframework.batch.sample.domain.Trade;
import org.springframework.batch.sample.item.writer.TradeWriter;

public class TradeProcessorTests extends TestCase {

	private MockControl writerControl;
	private TradeDao writer;
	private TradeWriter processor;
	
	public void setUp() {
		
		//create mock writer
		writerControl = MockControl.createControl(TradeDao.class);
		writer = (TradeDao)writerControl.getMock();
		
		//create processor
		processor = new TradeWriter();
		processor.setDao(writer);
	}
		
	public void testProcess() {
		
		Trade trade = new Trade();
		//set-up mock writer
		writer.writeTrade(trade);
		writerControl.replay();
		
		//call tested method
		processor.write(trade);
		
		//verify method calls
		writerControl.verify();
	}
	
	public void testProcessNonTradeObject() {
		
		writerControl.replay();
		//call tested method
		processor.write(this);
		writerControl.verify();
	}
}
