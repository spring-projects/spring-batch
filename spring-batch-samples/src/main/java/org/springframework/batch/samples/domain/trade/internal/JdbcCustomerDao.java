/*
 * Copyright 2006-2021 the original author or authors.
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

package org.springframework.batch.samples.domain.trade.internal;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.batch.samples.domain.trade.CustomerCredit;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.samples.domain.trade.CustomerDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

import javax.sql.DataSource;

/**
 * @author Lucas Ward
 * @author Mahmoud Ben Hassine
 * @author Yanming Zhou
 *
 */
public class JdbcCustomerDao implements CustomerDao {

	private static final String GET_CUSTOMER_BY_NAME = "SELECT ID, NAME, CREDIT from CUSTOMER where NAME = ?";

	private static final String INSERT_CUSTOMER = "INSERT into CUSTOMER(ID, NAME, CREDIT) values(?,?,?)";

	private static final String UPDATE_CUSTOMER = "UPDATE CUSTOMER set CREDIT = ? where NAME = ?";

	private DataFieldMaxValueIncrementer incrementer;

	private JdbcClient jdbcClient;

	public void setIncrementer(DataFieldMaxValueIncrementer incrementer) {
		this.incrementer = incrementer;
	}

	public void setDataSource(DataSource dataSource) {
		this.jdbcClient = JdbcClient.create(dataSource);
	}

	@Override
	public @Nullable CustomerCredit getCustomerByName(String name) {

		List<CustomerCredit> customers = jdbcClient.sql(GET_CUSTOMER_BY_NAME).param(1, name).query((rs, rowNum) -> {
			CustomerCredit customer = new CustomerCredit();
			customer.setName(rs.getString("NAME"));
			customer.setId(rs.getInt("ID"));
			customer.setCredit(rs.getBigDecimal("CREDIT"));
			return customer;
		}).list();

		if (customers.size() == 0) {
			return null;
		}
		else {
			return customers.get(0);
		}

	}

	@Override
	public void insertCustomer(String name, BigDecimal credit) {

		jdbcClient.sql(INSERT_CUSTOMER).params(incrementer.nextIntValue(), name, credit).update();
	}

	@Override
	public void updateCustomer(String name, BigDecimal credit) {
		jdbcClient.sql(UPDATE_CUSTOMER).params(credit, name).update();
	}

}
