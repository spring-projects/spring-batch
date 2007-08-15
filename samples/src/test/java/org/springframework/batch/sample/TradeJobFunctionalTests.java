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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.springframework.batch.sample.domain.Trade;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;



public class TradeJobFunctionalTests extends AbstractLifecycleSpringContextTests {

	private static final String GET_TRADES = "SELECT isin, quantity, price, customer FROM trade";
	private static final String GET_CUSTOMERS = "SELECT name, credit FROM customer";
	
	private ArrayList customers;
	private ArrayList trades;
	private int activeRow = 0;
	
	private JdbcTemplate jdbcTemplate;
	
	/**
	 * @param jdbcTemplate the jdbcTemplate to set
	 */
	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
	
	protected String[] getConfigLocations() {
		return new String[] {"jobs/tradeJob.xml"};
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.test.AbstractSingleSpringContextTests#onSetUp()
	 */
	protected void onSetUp() throws Exception {
		super.onSetUp();
		jdbcTemplate.update("delete from TRADE");
	}

	public void testLifecycle() throws Exception{
		super.testLifecycle();
	}

	protected void validatePostConditions() {
		
		// assertTrue(((Resource)applicationContext.getBean("customerFileLocator")).exists());
		
		customers = new ArrayList() {{add(new Customer("customer1", (100000 - 98.34)));
			add(new Customer("customer2", (100000 - 18.12 - 12.78)));
			add(new Customer("customer3", (100000 - 109.25)));
			add(new Customer("customer4", (100000 - 123.39)));}}; 

		trades = new ArrayList() {{add(new Trade("UK21341EAH45", 978, new BigDecimal("98.34"), "customer1"));
			add(new Trade("UK21341EAH46", 112, new BigDecimal("18.12"), "customer2"));
			add(new Trade("UK21341EAH47", 245, new BigDecimal("12.78"), "customer2"));
			add(new Trade("UK21341EAH48", 108, new BigDecimal("109.25"), "customer3"));
			add(new Trade("UK21341EAH49", 854, new BigDecimal("123.39"), "customer4"));}};
			
		// check content of the trade table
		jdbcTemplate.query(GET_TRADES, new RowCallbackHandler() {

			public void processRow(ResultSet rs) throws SQLException {
				Trade trade = (Trade)trades.get(activeRow++);
				
				assertTrue(trade.getIsin().equals(rs.getString(1)));
				assertTrue(trade.getQuantity() == rs.getLong(2));
				assertTrue(trade.getPrice().equals(rs.getBigDecimal(3)));
				assertTrue(trade.getCustomer().equals(rs.getString(4)));
			}
		});
		
		assertTrue(trades.size() == activeRow);
		
		// check content of the customer table
		activeRow = 0;
		jdbcTemplate.query(GET_CUSTOMERS, new RowCallbackHandler() {

			public void processRow(ResultSet rs) throws SQLException {
				Customer customer = (Customer)customers.get(activeRow++);
				
				assertTrue(customer.getName().equals(rs.getString(1)));
				assertTrue(customer.getCredit() == rs.getDouble(2));
			}
		});
		
		assertTrue(customers.size() == activeRow);
		
		// check content of the output file
		
		
//		 Clean up
		((FileSystemResource)applicationContext.getBean("customerFileLocator")).getFile().delete();
	}

	protected void validatePreConditions() {
		assertTrue(((Resource)applicationContext.getBean("fileLocator")).exists());
	}
	
	private class Customer {
		private String name;
		private double credit;
		
		public Customer(String name, double credit) {
			this.name = name;
			this.credit = credit;
		}
		
		public Customer(){
		}
		
		/**
		 * @return the credit
		 */
		public double getCredit() {
			return credit;
		}
		/**
		 * @param credit the credit to set
		 */
		public void setCredit(double credit) {
			this.credit = credit;
		}
		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}
		/**
		 * @param name the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			final int PRIME = 31;
			int result = 1;
			long temp;
			temp = Double.doubleToLongBits(credit);
			result = PRIME * result + (int) (temp ^ (temp >>> 32));
			result = PRIME * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
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
