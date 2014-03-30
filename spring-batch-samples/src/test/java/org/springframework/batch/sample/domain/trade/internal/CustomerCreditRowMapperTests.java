/*
 * Copyright 2008-2014 the original author or authors.
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

import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.batch.sample.support.AbstractRowMapperTests;
import org.springframework.jdbc.core.RowMapper;

public class CustomerCreditRowMapperTests extends AbstractRowMapperTests<CustomerCredit> {

	private static final int ID = 12;
	private static final String CUSTOMER = "Jozef Mak";
	private static final BigDecimal CREDIT = new BigDecimal("0.1");

	@Override
	protected CustomerCredit expectedDomainObject() {
		CustomerCredit credit = new CustomerCredit();
		credit.setId(ID);
		credit.setCredit(CREDIT);
		credit.setName(CUSTOMER);
		return credit;
	}

	@Override
	protected RowMapper<CustomerCredit> rowMapper() {
		return new CustomerCreditRowMapper();
	}

	@Override
	protected void setUpResultSetMock(ResultSet rs) throws SQLException {
		when(rs.getInt(CustomerCreditRowMapper.ID_COLUMN)).thenReturn(ID);
		when(rs.getString(CustomerCreditRowMapper.NAME_COLUMN)).thenReturn(CUSTOMER);
		when(rs.getBigDecimal(CustomerCreditRowMapper.CREDIT_COLUMN)).thenReturn(CREDIT);
	}
}
