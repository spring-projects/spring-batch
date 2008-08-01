/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.sample.domain.trade.internal;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.sample.domain.trade.CustomerDebit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class JdbcCustomerDebitDaoTests {

	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Autowired
	private JdbcCustomerDebitDao writer;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	@Transactional @Test
	public void testWrite() {

		//insert customer credit 
		simpleJdbcTemplate.getJdbcOperations().execute("INSERT INTO customer VALUES (99, 0, 'testName', 100)");

		//create customer debit
		CustomerDebit customerDebit = new CustomerDebit();
		customerDebit.setName("testName");
		customerDebit.setDebit(BigDecimal.valueOf(5));

		//call writer
		writer.write(customerDebit);

		//verify customer credit
		simpleJdbcTemplate.getJdbcOperations().query("SELECT name, credit FROM customer WHERE name = 'testName'",
				new RowCallbackHandler() {
					public void processRow(ResultSet rs) throws SQLException {
						assertEquals(95, rs.getLong("credit"));
					}
				});

	}
}
