package org.springframework.batch.sample.tasklet;

import java.math.BigDecimal;

import junit.framework.TestCase;

import org.springframework.batch.io.file.DefaultFlatFileItemReader;
import org.springframework.batch.sample.dao.TradeDao;
import org.springframework.batch.sample.domain.Trade;

public class SimpleTradeTaskletTests extends TestCase {

	private boolean inputCalled = false;
	private boolean writerCalled = false;

	public void testReadAndProcess() throws Exception {

		//create input
		DefaultFlatFileItemReader input = new DefaultFlatFileItemReader() {

			private boolean done = false;

			public Object read() {
				if (!done) {
					Trade trade = new Trade("1234", 5, new BigDecimal(100), "testName");
					inputCalled = true;
					done = true;
					return trade;
				} else {
					return null;
				}
			}
		};

		//create writer
		TradeDao dao = new TradeDao() {
			public void writeTrade(Trade trade) {
				assertEquals("1234",trade.getIsin());
				assertEquals(5, trade.getQuantity());
				assertEquals(new BigDecimal(100), trade.getPrice());
				assertEquals("testName", trade.getCustomer());
				writerCalled = true;
			}
		};

		//create module
		SimpleTradeTasklet module = new SimpleTradeTasklet();
		module.setItemReader(input);
		module.setTradeDao(dao);

		//call tested methods
		//read method should return true, because input returned fieldset
		assertTrue(module.execute().isContinuable());

		//verify whether input and writer were called
		assertTrue(inputCalled);
		assertTrue(writerCalled);

		//read should return false, because input returned null
		assertFalse(module.execute().isContinuable());
	}
}
