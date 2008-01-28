package org.springframework.batch.sample.dao;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.batch.sample.domain.Trade;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.support.incrementer.AbstractDataFieldMaxValueIncrementer;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;

public class JdbcTradeWriterTests extends AbstractTransactionalDataSourceSpringContextTests {

	protected String[] getConfigLocations() {
		return new String[] { "data-source-context.xml" };
	}

	public void testWrite() {
		
		JdbcTradeDao writer = new JdbcTradeDao();
		
		AbstractDataFieldMaxValueIncrementer incrementer = (AbstractDataFieldMaxValueIncrementer)applicationContext.getBean("jobIncrementer");
		incrementer.setIncrementerName("TRADE_SEQ");
		
		writer.setIncrementer(incrementer);
		writer.setJdbcTemplate(jdbcTemplate);
		
		Trade trade = new Trade();
		trade.setCustomer("testCustomer");
		trade.setIsin("5647238492");
		trade.setPrice(new BigDecimal(Double.toString(99.69)));
		trade.setQuantity(5);
		
		writer.writeTrade(trade);
		
		jdbcTemplate.query("SELECT * FROM TRADE WHERE ISIN = '5647238492'", new RowCallbackHandler() {
			public void processRow(ResultSet rs) throws SQLException {
				assertEquals("testCustomer", rs.getString("CUSTOMER"));
				assertEquals(new BigDecimal(Double.toString(99.69)), rs.getBigDecimal("PRICE"));
				assertEquals(5,rs.getLong("QUANTITY"));
			}
		});
	}
}
