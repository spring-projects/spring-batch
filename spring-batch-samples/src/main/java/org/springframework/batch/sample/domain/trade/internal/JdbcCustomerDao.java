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

package org.springframework.batch.sample.domain.trade.internal;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.batch.sample.domain.trade.CustomerDao;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

/**
 * @author Lucas Ward
 * @author Mahmoud Ben Hassine
 *
 */
public class JdbcCustomerDao extends JdbcDaoSupport implements CustomerDao {

	private static final String GET_CUSTOMER_BY_NAME = "SELECT ID, NAME, CREDIT from CUSTOMER where NAME = ?";

	private static final String INSERT_CUSTOMER = "INSERT into CUSTOMER(ID, NAME, CREDIT) values(?,?,?)";

	private static final String UPDATE_CUSTOMER = "UPDATE CUSTOMER set CREDIT = ? where NAME = ?";

	private DataFieldMaxValueIncrementer incrementer;

	public void setIncrementer(DataFieldMaxValueIncrementer incrementer) {
		this.incrementer = incrementer;
	}

	@Override
	public CustomerCredit getCustomerByName(String name) {

		List<CustomerCredit> customers = getJdbcTemplate().query(GET_CUSTOMER_BY_NAME, (rs, rowNum) -> {
			CustomerCredit customer = new CustomerCredit();
			customer.setName(rs.getString("NAME"));
			customer.setId(rs.getInt("ID"));
			customer.setCredit(rs.getBigDecimal("CREDIT"));
			return customer;
		}, name);

		if (customers.size() == 0) {
			return null;
		}
		else {
			return customers.get(0);
		}

	}

	@Override
	public void insertCustomer(String name, BigDecimal credit) {

		getJdbcTemplate().update(INSERT_CUSTOMER, new Object[] { incrementer.nextIntValue(), name, credit });
	}

	@Override
	public void updateCustomer(String name, BigDecimal credit) {
		getJdbcTemplate().update(UPDATE_CUSTOMER, new Object[] { credit, name });
	}

}
