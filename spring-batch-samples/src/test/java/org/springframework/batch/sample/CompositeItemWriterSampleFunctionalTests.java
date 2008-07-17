package org.springframework.batch.sample;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.springframework.batch.sample.domain.Trade;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowCallbackHandler;


public class CompositeItemWriterSampleFunctionalTests extends AbstractValidatingBatchLauncherTests {

	private static final String GET_TRADES = "SELECT isin, quantity, price, customer FROM trade order by isin";
	
	private static final String EXPECTED_OUTPUT_FILE = 
		"Trade: [isin=UK21341EAH41,quantity=211,price=31.11,customer=customer1]" +
		"Trade: [isin=UK21341EAH42,quantity=212,price=32.11,customer=customer2]" +
		"Trade: [isin=UK21341EAH43,quantity=213,price=33.11,customer=customer3]" +
		"Trade: [isin=UK21341EAH44,quantity=214,price=34.11,customer=customer4]" +
		"Trade: [isin=UK21341EAH45,quantity=215,price=35.11,customer=customer5]";
	
	private JdbcOperations jdbcTemplate;
	
	private int activeRow = 0;
	
	private int before;
	
	/* (non-Javadoc)
	 * @see org.springframework.batch.sample.AbstractLifecycleSpringContextTests#validatePreConditions()
	 */
	protected void validatePreConditions() throws Exception {
		jdbcTemplate.update("DELETE from TRADE");
		before = jdbcTemplate.queryForInt("SELECT COUNT(*) from TRADE");
	}
	
	protected void validatePostConditions() throws Exception {	
		checkOutputFile();
		checkOutputTable();
	}
	
	private void checkOutputTable() {
		final List<Trade> trades = new ArrayList<Trade>() {{
				add(new Trade("UK21341EAH41", 211, new BigDecimal("31.11"), "customer1"));
				add(new Trade("UK21341EAH42", 212, new BigDecimal("32.11"), "customer2"));
				add(new Trade("UK21341EAH43", 213, new BigDecimal("33.11"), "customer3"));
				add(new Trade("UK21341EAH44", 214, new BigDecimal("34.11"), "customer4"));
				add(new Trade("UK21341EAH45", 215, new BigDecimal("35.11"), "customer5"));
		}};

		int after = jdbcTemplate.queryForInt("SELECT COUNT(*) from TRADE");
		
		assertEquals(before+5, after);
		
		jdbcTemplate.query(GET_TRADES, new RowCallbackHandler() {
			public void processRow(ResultSet rs) throws SQLException {
				Trade trade = (Trade)trades.get(activeRow++);
				
				assertEquals(trade.getIsin(), rs.getString(1));
				assertEquals(trade.getQuantity(), rs.getLong(2));
				assertEquals(trade.getPrice(), rs.getBigDecimal(3));
				assertEquals(trade.getCustomer(), rs.getString(4));
			}
		});
		
	}

	@SuppressWarnings("unchecked")
	private void checkOutputFile() throws FileNotFoundException, IOException {
		List<String> outputLines = IOUtils.readLines(
				new FileInputStream("target/test-outputs/20070122.testStream.ParallelCustomerReportStep.TEMP.txt"));
		
		String output = "";
		for (String line : outputLines) {
			output += line;
		}
		
		assertEquals(EXPECTED_OUTPUT_FILE, output);
	}
	
	public void setJdbcTemplate(JdbcOperations jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
}
