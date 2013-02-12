package org.springframework.batch.sample.domain.trade.internal;

import static org.mockito.Mockito.mock;

import java.util.Collections;

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
		writer = mock(TradeDao.class);
		
		//create processor
		processor = new TradeWriter();
		processor.setDao(writer);
	}
		
	@Test
	public void testProcess() {
		
		Trade trade = new Trade();
		//set-up mock writer
		writer.writeTrade(trade);
		
		//call tested method
		processor.write(Collections.singletonList(trade));
	}
	
}
