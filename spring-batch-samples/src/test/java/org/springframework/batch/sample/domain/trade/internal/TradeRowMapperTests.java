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

import org.springframework.batch.sample.domain.trade.Trade;
import org.springframework.batch.sample.support.AbstractRowMapperTests;
import org.springframework.jdbc.core.RowMapper;

public class TradeRowMapperTests extends AbstractRowMapperTests<Trade> {

	private static final String ISIN = "jsgk342";
	private static final long QUANTITY = 0;
	private static final BigDecimal PRICE = new BigDecimal("1.1");
	private static final String CUSTOMER = "Martin Hrancok";

	@Override
	protected Trade expectedDomainObject() {
		Trade trade = new Trade();
		trade.setIsin(ISIN);
		trade.setQuantity(QUANTITY);
		trade.setPrice(PRICE);
		trade.setCustomer(CUSTOMER);

		return trade;
	}

	@Override
	protected RowMapper<Trade> rowMapper() {
		return new TradeRowMapper();
	}

	@Override
	protected void setUpResultSetMock(ResultSet rs) throws SQLException {
		when(rs.getLong(TradeRowMapper.ID_COLUMN)).thenReturn(12L);
		when(rs.getString(TradeRowMapper.ISIN_COLUMN)).thenReturn(ISIN);
		when(rs.getLong(TradeRowMapper.QUANTITY_COLUMN)).thenReturn(QUANTITY);
		when(rs.getBigDecimal(TradeRowMapper.PRICE_COLUMN)).thenReturn(PRICE);
		when(rs.getString(TradeRowMapper.CUSTOMER_COLUMN)).thenReturn(CUSTOMER);
		when(rs.getInt(TradeRowMapper.VERSION_COLUMN)).thenReturn(0);
	}
}
