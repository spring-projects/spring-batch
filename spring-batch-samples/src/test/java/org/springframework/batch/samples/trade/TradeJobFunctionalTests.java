/*
 * Copyright 2006-2025 the original author or authors.
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

package org.springframework.batch.samples.trade;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.samples.domain.trade.Trade;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringJUnitConfig(
		locations = { "/org/springframework/batch/samples/trade/job/tradeJob.xml", "/simple-job-launcher-context.xml" })
class TradeJobFunctionalTests {

	private static final String GET_TRADES = "select ISIN, QUANTITY, PRICE, CUSTOMER, ID, VERSION from TRADE order by ISIN";

	private static final String GET_CUSTOMERS = "select NAME, CREDIT from CUSTOMER order by NAME";

	private List<Customer> customers;

	private List<Trade> trades;

	private int activeRow = 0;

	private JdbcTemplate jdbcTemplate;

	private final Map<String, Double> credits = new HashMap<>();

	@Autowired
	private JobOperatorTestUtils jobOperatorTestUtils;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@BeforeEach
	void onSetUp() {
		JdbcTestUtils.deleteFromTables(jdbcTemplate, "TRADE");
		List<Map<String, Object>> list = jdbcTemplate.queryForList("select NAME, CREDIT from CUSTOMER");

		for (Map<String, Object> map : list) {
			credits.put((String) map.get("NAME"), ((Number) map.get("CREDIT")).doubleValue());
		}
	}

	@AfterEach
	void tearDown() {
		JdbcTestUtils.deleteFromTables(jdbcTemplate, "TRADE");
	}

	@Test
	void testLaunchJob() throws Exception {
		this.jobOperatorTestUtils.startJob();

		customers = Arrays.asList(new Customer("customer1", (credits.get("customer1") - 98.34)),
				new Customer("customer2", (credits.get("customer2") - 18.12 - 12.78)),
				new Customer("customer3", (credits.get("customer3") - 109.25)),
				new Customer("customer4", credits.get("customer4") - 123.39));

		trades = Arrays.asList(new Trade("UK21341EAH45", 978, new BigDecimal("98.34"), "customer1"),
				new Trade("UK21341EAH46", 112, new BigDecimal("18.12"), "customer2"),
				new Trade("UK21341EAH47", 245, new BigDecimal("12.78"), "customer2"),
				new Trade("UK21341EAH48", 108, new BigDecimal("109.25"), "customer3"),
				new Trade("UK21341EAH49", 854, new BigDecimal("123.39"), "customer4"));

		jdbcTemplate.query(GET_TRADES, rs -> {
			Trade trade = trades.get(activeRow++);

			assertEquals(trade.getIsin(), rs.getString(1));
			assertEquals(trade.getQuantity(), rs.getLong(2));
			assertEquals(trade.getPrice(), rs.getBigDecimal(3));
			assertEquals(trade.getCustomer(), rs.getString(4));
		});

		assertEquals(activeRow, trades.size());

		activeRow = 0;
		jdbcTemplate.query(GET_CUSTOMERS, rs -> {
			Customer customer = customers.get(activeRow++);

			assertEquals(customer.getName(), rs.getString(1));
			assertEquals(customer.getCredit(), rs.getDouble(2), .01);
		});

		assertEquals(customers.size(), activeRow);
	}

	private static class Customer {

		private final String name;

		private final double credit;

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
			}
			else if (!name.equals(other.name))
				return false;
			return true;
		}

	}

}
