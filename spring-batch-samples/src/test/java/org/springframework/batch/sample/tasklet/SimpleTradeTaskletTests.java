package org.springframework.batch.sample.tasklet;

import java.math.BigDecimal;

import junit.framework.TestCase;

import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.sample.dao.TradeDao;
import org.springframework.batch.sample.domain.Trade;

public class SimpleTradeTaskletTests extends TestCase {

	private boolean inputCalled = false;
	private boolean writerCalled = false;

	public void testReadAndProcess() throws Exception {

		//create input
		FlatFileItemReader input = new FlatFileItemReader() {

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
		SimpleTradeWriter module = new SimpleTradeWriter();
		module.setTradeDao(dao);

		module.write(input.read());

		//verify whether input and writer were called
		assertTrue(inputCalled);
		assertTrue(writerCalled);

	}
}
