/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.sample.domain.trade.Trade;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/jobs/tradeJob.xml",
		"/job-runner-context.xml" })
public class TradeJobFunctionalTests {

	private static final String GET_TRADES = "select ISIN, QUANTITY, PRICE, CUSTOMER, ID, VERSION from TRADE order by ISIN";
	private static final String GET_CUSTOMERS = "select NAME, CREDIT from CUSTOMER order by NAME";
	
	private List<Customer> customers;
	private List<Trade> trades;
	private int activeRow = 0;
	
	private SimpleJdbcTemplate simpleJdbcTemplate;
	private Map<String, Double> credits = new HashMap<String, Double>();

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}
	
	@Before
	public void onSetUp() throws Exception {
		simpleJdbcTemplate.update("delete from TRADE");
		List<Map<String, Object>> list = simpleJdbcTemplate.queryForList("select NAME, CREDIT from CUSTOMER");
		for (Map<String, Object> map : list) {
			credits.put((String) map.get("NAME"), ((Number) map.get("CREDIT")).doubleValue());
		}
	}
	
	@After
	public void tearDown() throws Exception {
		simpleJdbcTemplate.update("delete from TRADE");
	}

	@Test
	public void testLaunchJob() throws Exception {
		
		jobLauncherTestUtils.launchJob();
		
		customers = Arrays.asList(new Customer("customer1", (credits.get("customer1") - 98.34)),
				new Customer("customer2", (credits.get("customer2") - 18.12 - 12.78)),
				new Customer("customer3", (credits.get("customer3") - 109.25)),
				new Customer("customer4", credits.get("customer4") - 123.39));

		trades = Arrays.asList(new Trade("UK21341EAH45", 978, new BigDecimal("98.34"), "customer1"),
				new Trade("UK21341EAH46", 112, new BigDecimal("18.12"), "customer2"),
				new Trade("UK21341EAH47", 245, new BigDecimal("12.78"), "customer2"),
				new Trade("UK21341EAH48", 108, new BigDecimal("109.25"), "customer3"),
				new Trade("UK21341EAH49", 854, new BigDecimal("123.39"), "customer4"));
			
		// check content of the trade table
		simpleJdbcTemplate.getJdbcOperations().query(GET_TRADES, new RowCallbackHandler() {

			public void processRow(ResultSet rs) throws SQLException {
				Trade trade = trades.get(activeRow++);
				
				assertTrue(trade.getIsin().equals(rs.getString(1)));
				assertTrue(trade.getQuantity() == rs.getLong(2));
				assertTrue(trade.getPrice().equals(rs.getBigDecimal(3)));
				assertTrue(trade.getCustomer().equals(rs.getString(4)));
			}
		});
		
		assertEquals(activeRow, trades.size());
		
		// check content of the customer table
		activeRow = 0;
		simpleJdbcTemplate.getJdbcOperations().query(GET_CUSTOMERS, new RowCallbackHandler() {

			public void processRow(ResultSet rs) throws SQLException {
				Customer customer = customers.get(activeRow++);
				
				assertEquals(customer.getName(),rs.getString(1));
				assertEquals(customer.getCredit(), rs.getDouble(2), .01);
			}
		});
		
		assertEquals(customers.size(), activeRow);
		
		// check content of the output file
	}

	private static class Customer {
		private String name;
		private double credit;
		
		public Customer(String name, double credit) {
			this.name = name;
			this.credit = credit;
		}
		
		/**
		 * @return the credit
		 */
		public double getCredit() {
			return credit;
		}

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		@Override
		public int hashCode() {
			final int PRIME = 31;
			int result = 1;
			long temp;
			temp = Double.doubleToLongBits(credit);
			result = PRIME * result + (int) (temp ^ (temp >>> 32));
			result = PRIME * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final Customer other = (Customer) obj;
			if (Double.doubleToLongBits(credit) != Double.doubleToLongBits(other.credit))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
		
		
	}
	
	
}
