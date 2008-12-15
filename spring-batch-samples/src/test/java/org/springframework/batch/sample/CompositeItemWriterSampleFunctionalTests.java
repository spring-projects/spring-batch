package org.springframework.batch.sample;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.junit.runner.RunWith;
import org.springframework.batch.sample.domain.trade.Trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class CompositeItemWriterSampleFunctionalTests extends AbstractValidatingBatchLauncherTests {

	private static final String GET_TRADES = "SELECT isin, quantity, price, customer FROM trade order by isin";

	private static final String EXPECTED_OUTPUT_FILE = "Trade: [isin=UK21341EAH41,quantity=211,price=31.11,customer=customer1]"
			+ "Trade: [isin=UK21341EAH42,quantity=212,price=32.11,customer=customer2]"
			+ "Trade: [isin=UK21341EAH43,quantity=213,price=33.11,customer=customer3]"
			+ "Trade: [isin=UK21341EAH44,quantity=214,price=34.11,customer=customer4]"
			+ "Trade: [isin=UK21341EAH45,quantity=215,price=35.11,customer=customer5]";

	private SimpleJdbcTemplate simpleJdbcTemplate;

	private int activeRow = 0;

	private int before;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	@Override
	protected void validatePreConditions() throws Exception {
		simpleJdbcTemplate.update("DELETE from TRADE");
		before = simpleJdbcTemplate.queryForInt("SELECT COUNT(*) from TRADE");
	}

	@Override
	protected void validatePostConditions() throws Exception {
		checkOutputFile("target/test-outputs/CustomerReport1.txt");
		checkOutputFile("target/test-outputs/CustomerReport2.txt");
		checkOutputTable();
	}

	private void checkOutputTable() {
		final List<Trade> trades = new ArrayList<Trade>() {
			{
				add(new Trade("UK21341EAH41", 211, new BigDecimal("31.11"), "customer1"));
				add(new Trade("UK21341EAH42", 212, new BigDecimal("32.11"), "customer2"));
				add(new Trade("UK21341EAH43", 213, new BigDecimal("33.11"), "customer3"));
				add(new Trade("UK21341EAH44", 214, new BigDecimal("34.11"), "customer4"));
				add(new Trade("UK21341EAH45", 215, new BigDecimal("35.11"), "customer5"));
			}
		};

		int after = simpleJdbcTemplate.queryForInt("SELECT COUNT(*) from TRADE");

		assertEquals(before + 5, after);

		simpleJdbcTemplate.getJdbcOperations().query(GET_TRADES, new RowCallbackHandler() {
			public void processRow(ResultSet rs) throws SQLException {
				Trade trade = trades.get(activeRow++);

				assertEquals(trade.getIsin(), rs.getString(1));
				assertEquals(trade.getQuantity(), rs.getLong(2));
				assertEquals(trade.getPrice(), rs.getBigDecimal(3));
				assertEquals(trade.getCustomer(), rs.getString(4));
			}
		});

	}

	private void checkOutputFile(String fileName) throws IOException {
		@SuppressWarnings("unchecked")
		List<String> outputLines = IOUtils.readLines(new FileInputStream(fileName));

		String output = "";
		for (String line : outputLines) {
			output += line;
		}

		assertEquals(EXPECTED_OUTPUT_FILE, output);
	}

}
