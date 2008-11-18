package org.springframework.batch.sample.domain.trade;

import static org.junit.Assert.*;

import java.math.BigDecimal;

import org.junit.Test;

public class TradeTests {

	@Test
	public void testEquality(){
		
		Trade trade1 = new Trade("isin", 1, new BigDecimal(1.1), "customer1");
		Trade trade1Clone = new Trade("isin", 1, new BigDecimal(1.1), "customer1");
		Trade trade2 = new Trade("isin", 1, new BigDecimal(2.3), "customer2");
		
		assertEquals(trade1, trade1Clone);
		assertFalse(trade1.equals(trade2));
	}
}
