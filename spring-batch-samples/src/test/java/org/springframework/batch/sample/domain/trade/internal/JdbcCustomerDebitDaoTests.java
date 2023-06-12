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
package org.springframework.batch.sample.domain.trade.internal;

import java.math.BigDecimal;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.batch.sample.domain.trade.CustomerDebit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringJUnitConfig
class JdbcCustomerDebitDaoTests {

	private JdbcOperations jdbcTemplate;

	@Autowired
	private JdbcCustomerDebitDao writer;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Test
	@Transactional
	void testWrite() {
		jdbcTemplate.execute("INSERT INTO CUSTOMER VALUES (99, 0, 'testName', 100)");

		CustomerDebit customerDebit = new CustomerDebit();
		customerDebit.setName("testName");
		customerDebit.setDebit(BigDecimal.valueOf(5));

		writer.write(customerDebit);

		jdbcTemplate.query("SELECT name, credit FROM CUSTOMER WHERE name = 'testName'", rs -> {
			assertEquals(95, rs.getLong("credit"));
		});
	}

}
