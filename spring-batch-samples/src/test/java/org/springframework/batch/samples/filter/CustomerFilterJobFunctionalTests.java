/*
 * Copyright 2006-2023 the original author or authors.
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

package org.springframework.batch.samples.filter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringJUnitConfig(locations = { "/org/springframework/batch/samples/filter/job/customerFilterJob.xml",
		"/simple-job-launcher-context.xml" })
class CustomerFilterJobFunctionalTests {

	private static final String GET_CUSTOMERS = "select NAME, CREDIT from CUSTOMER order by NAME";

	private List<Customer> customers;

	private int activeRow = 0;

	private JdbcTemplate jdbcTemplate;

	private final Map<String, Double> credits = new HashMap<>();

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@BeforeEach
	void onSetUp() {
		JdbcTestUtils.deleteFromTables(jdbcTemplate, "TRADE");
		JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "CUSTOMER", "ID > 4");
		jdbcTemplate.update("update CUSTOMER set credit=100000");

		List<Map<String, Object>> list = jdbcTemplate.queryForList("select name, CREDIT from CUSTOMER");

		for (Map<String, Object> map : list) {
			credits.put((String) map.get("NAME"), ((Number) map.get("CREDIT")).doubleValue());
		}
	}

	@AfterEach
	void tearDown() {
		JdbcTestUtils.deleteFromTables(jdbcTemplate, "TRADE");
		JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "CUSTOMER", "ID > 4");
	}

	@Test
	void testFilterJob() throws Exception {
		JobExecution jobExecution = jobLauncherTestUtils.launchJob();

		customers = Arrays.asList(new Customer("customer1", (credits.get("customer1"))),
				new Customer("customer2", (credits.get("customer2"))), new Customer("customer3", 100500),
				new Customer("customer4", credits.get("customer4")), new Customer("customer5", 32345),
				new Customer("customer6", 123456));

		activeRow = 0;
		jdbcTemplate.query(GET_CUSTOMERS, rs -> {
			Customer customer = customers.get(activeRow++);
			assertEquals(customer.getName(), rs.getString(1));
			assertEquals(customer.getCredit(), rs.getDouble(2), .01);
		});

		Map<String, Object> step1Execution = this.getStepExecution(jobExecution, "uploadCustomer");
		assertEquals("4", step1Execution.get("READ_COUNT").toString());
		assertEquals("1", step1Execution.get("FILTER_COUNT").toString());
		assertEquals("3", step1Execution.get("WRITE_COUNT").toString());
	}

	private Map<String, Object> getStepExecution(JobExecution jobExecution, String stepName) {
		Long jobExecutionId = jobExecution.getId();
		return jdbcTemplate.queryForMap(
				"SELECT * from BATCH_STEP_EXECUTION where JOB_EXECUTION_ID = ? and STEP_NAME = ?", jobExecutionId,
				stepName);
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
