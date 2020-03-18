/*
 * Copyright 2008-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.sample;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.sample.domain.trade.Trade;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/jobs/compositeItemWriterSampleJob.xml", "/job-runner-context.xml" })
public class CompositeItemWriterSampleFunctionalTests {
	private static final String GET_TRADES = "SELECT isin, quantity, price, customer FROM TRADE order by isin";
	private static final String EXPECTED_OUTPUT_FILE = "Trade: [isin=UK21341EAH41,quantity=211,price=31.11,customer=customer1]"
			+ "Trade: [isin=UK21341EAH42,quantity=212,price=32.11,customer=customer2]"
			+ "Trade: [isin=UK21341EAH43,quantity=213,price=33.11,customer=customer3]"
			+ "Trade: [isin=UK21341EAH44,quantity=214,price=34.11,customer=customer4]"
			+ "Trade: [isin=UK21341EAH45,quantity=215,price=35.11,customer=customer5]";

	private JdbcOperations jdbcTemplate;

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Test
	public void testJobLaunch() throws Exception {
        jdbcTemplate.update("DELETE from TRADE");
		int before = jdbcTemplate.queryForObject("SELECT COUNT(*) from TRADE", Integer.class);

		jobLauncherTestUtils.launchJob();

		checkOutputFile("build/test-outputs/CustomerReport1.txt");
		checkOutputFile("build/test-outputs/CustomerReport2.txt");
		checkOutputTable(before);
	}

	private void checkOutputTable(int before) {
		@SuppressWarnings("serial")
		final List<Trade> trades = new ArrayList<Trade>() {
			{
				add(new Trade("UK21341EAH41", 211, new BigDecimal("31.11"), "customer1"));
				add(new Trade("UK21341EAH42", 212, new BigDecimal("32.11"), "customer2"));
				add(new Trade("UK21341EAH43", 213, new BigDecimal("33.11"), "customer3"));
				add(new Trade("UK21341EAH44", 214, new BigDecimal("34.11"), "customer4"));
				add(new Trade("UK21341EAH45", 215, new BigDecimal("35.11"), "customer5"));
			}
		};

		int after = jdbcTemplate.queryForObject("SELECT COUNT(*) from TRADE", Integer.class);

		assertEquals(before + 5, after);

        jdbcTemplate.query(GET_TRADES, new RowCallbackHandler() {
			private int activeRow = 0;

			@Override
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
		@SuppressWarnings("resource")
		List<String> outputLines = IOUtils.readLines(new FileInputStream(fileName), "UTF-8");

		String output = "";
		for (String line : outputLines) {
			output += line;
		}

		assertEquals(EXPECTED_OUTPUT_FILE, output);
	}
}
