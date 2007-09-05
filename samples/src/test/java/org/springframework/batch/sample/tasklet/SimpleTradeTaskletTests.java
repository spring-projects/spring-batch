package org.springframework.batch.sample.tasklet;

import java.math.BigDecimal;

import junit.framework.TestCase;

import org.springframework.batch.io.file.FieldSet;
import org.springframework.batch.io.file.support.DefaultFlatFileInputSource;
import org.springframework.batch.sample.dao.TradeWriter;
import org.springframework.batch.sample.domain.Trade;
import org.springframework.batch.sample.tasklet.SimpleTradeTasklet;

public class SimpleTradeTaskletTests extends TestCase {

	private boolean inputCalled = false;
	private boolean writerCalled = false;

	public void testReadAndProcess() throws Exception {
		
		//create input
		DefaultFlatFileInputSource input = new DefaultFlatFileInputSource() {
			
			private boolean done = false;
			
			public FieldSet readFieldSet() {
				if (!done) {
					FieldSet fs = new FieldSet(new String[] {"1234","5","100","testName"},
							   new String[] {"ISIN", "quantity", "price", "customer"});
					inputCalled = true;
					done = true;
					return fs;
				} else {
					return null;
				}
			}
		};
		
		//create writer
		TradeWriter writer = new TradeWriter() {
			public void writeTrade(Trade trade) {
				assertEquals("1234",trade.getIsin());
				assertEquals(5, trade.getQuantity());
				assertEquals(new BigDecimal(100), trade.getPrice());
				assertEquals("testName", trade.getCustomer());
				writerCalled = true;
			}
			public void write(Object output) {}
			public void close() {}
			public void open() {}			
		};
		
		//create module
		SimpleTradeTasklet module = new SimpleTradeTasklet();
		module.setInputSource(input);
		module.setTradeDao(writer);
		
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
