package org.springframework.batch.sample.domain.trade.internal;

import static org.easymock.EasyMock.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.sample.domain.trade.Trade;
import org.springframework.batch.sample.domain.trade.TradeDao;

public class TradeProcessorTests {

	private TradeDao writer;
	private TradeWriter processor;
	
	@Before
	public void setUp() {
		
		//create mock writer
		writer = createMock(TradeDao.class);
		
		//create processor
		processor = new TradeWriter();
		processor.setDao(writer);
	}
		
	@Test
	public void testProcess() {
		
		Trade trade = new Trade();
		//set-up mock writer
		writer.writeTrade(trade);
		replay(writer);
		
		//call tested method
		processor.write(trade);
		
		//verify method calls
		verify(writer);
	}
	
}
