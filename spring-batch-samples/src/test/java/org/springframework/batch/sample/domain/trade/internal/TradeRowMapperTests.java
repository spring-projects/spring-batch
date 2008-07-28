package org.springframework.batch.sample.domain.trade.internal;

import static org.easymock.EasyMock.*;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.batch.sample.domain.trade.Trade;
import org.springframework.batch.sample.support.AbstractRowMapperTests;
import org.springframework.jdbc.core.RowMapper;

public class TradeRowMapperTests extends AbstractRowMapperTests {

	private static final String ISIN = "jsgk342";
	private static final long QUANTITY = 0;
	private static final BigDecimal PRICE = new BigDecimal(1.1);
	private static final String CUSTOMER = "Martin Hrancok";

	protected Object expectedDomainObject() {
		Trade trade = new Trade();
		trade.setIsin(ISIN);
		trade.setQuantity(QUANTITY);
		trade.setPrice(PRICE);
		trade.setCustomer(CUSTOMER);
		return trade;
	}

	protected RowMapper rowMapper() {
		return new TradeRowMapper();
	}

	protected void setUpResultSetMock(ResultSet rs) throws SQLException {
		expect(rs.getString(TradeRowMapper.ISIN_COLUMN)).andReturn(ISIN);
		expect(rs.getLong(TradeRowMapper.QUANTITY_COLUMN)).andReturn(QUANTITY);
		expect(rs.getBigDecimal(TradeRowMapper.PRICE_COLUMN)).andReturn(PRICE);
		expect(rs.getString(TradeRowMapper.CUSTOMER_COLUMN)).andReturn(CUSTOMER);
	}

}
