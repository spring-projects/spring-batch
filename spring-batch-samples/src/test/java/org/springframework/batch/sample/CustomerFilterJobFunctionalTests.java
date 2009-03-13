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
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.AbstractJobTests;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class CustomerFilterJobFunctionalTests extends AbstractJobTests {

	private static final String GET_CUSTOMERS = "select NAME, CREDIT from CUSTOMER order by NAME";

	private List<Customer> customers;
	private int activeRow = 0;

	private SimpleJdbcTemplate simpleJdbcTemplate;
	private Map<String, Double> credits = new HashMap<String, Double>();

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	@Before
	public void onSetUp() throws Exception {
		simpleJdbcTemplate.update("delete from TRADE");
		simpleJdbcTemplate.update("delete from CUSTOMER where ID > 4");
		List<Map<String, Object>> list = simpleJdbcTemplate.queryForList("select name, CREDIT from customer");
		for (Map<String, Object> map : list) {
			credits.put((String) map.get("NAME"), ((Number) map.get("CREDIT")).doubleValue());
		}
	}

	@After
	public void tearDown() throws Exception {
		simpleJdbcTemplate.update("delete from TRADE");
		simpleJdbcTemplate.update("delete from CUSTOMER where ID > 4");
	}

	@Test
	public void testFilterJob() throws Exception {

		JobExecution jobExecution = this.launchJob();

		customers = Arrays.asList(new Customer("customer1", (credits.get("customer1"))), new Customer("customer2",
				(credits.get("customer2"))), new Customer("customer3", 100500), new Customer("customer4", credits
				.get("customer4")), new Customer("customer5", 32345), new Customer("customer6", 123456));

		// check content of the customer table
		activeRow = 0;
		simpleJdbcTemplate.getJdbcOperations().query(GET_CUSTOMERS, new RowCallbackHandler() {

			public void processRow(ResultSet rs) throws SQLException {
				Customer customer = customers.get(activeRow++);
				assertEquals(customer.getName(), rs.getString(1));
				assertEquals(customer.getCredit(), rs.getDouble(2), .01);
			}
		});

		Map<String, Object> step1Execution = this.getStepExecution(jobExecution.getId(), "uploadCustomer");
		assertEquals(new Long(4), step1Execution.get("READ_COUNT"));
		assertEquals(new Long(1), step1Execution.get("FILTER_COUNT"));
		assertEquals(new Long(3), step1Execution.get("WRITE_COUNT"));

	}

	private Map<String, Object> getStepExecution(long jobExecutionId, String stepName) {
		return simpleJdbcTemplate.queryForMap(
				"SELECT * from BATCH_STEP_EXECUTION where JOB_EXECUTION_ID = ? and STEP_NAME = ?", jobExecutionId,
				stepName);
	}

	private static class Customer {
		private String name;
		private double credit;

		public Customer(String name, double credit) {
			this.name = name;
			this.credit = credit;
		}

		public Customer() {
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

		/*
		 * (non-Javadoc)
		 * 
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

		/*
		 * (non-Javadoc)
		 * 
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
			}
			else if (!name.equals(other.name))
				return false;
			return true;
		}

	}

}
