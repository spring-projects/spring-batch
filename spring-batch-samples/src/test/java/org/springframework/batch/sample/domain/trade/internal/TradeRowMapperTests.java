package org.springframework.batch.sample.domain.trade.internal;

import static org.mockito.Mockito.when;

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
		when(rs.getLong(TradeRowMapper.ID_COLUMN)).thenReturn(12L);
		when(rs.getString(TradeRowMapper.ISIN_COLUMN)).thenReturn(ISIN);
		when(rs.getLong(TradeRowMapper.QUANTITY_COLUMN)).thenReturn(QUANTITY);
		when(rs.getBigDecimal(TradeRowMapper.PRICE_COLUMN)).thenReturn(PRICE);
		when(rs.getString(TradeRowMapper.CUSTOMER_COLUMN)).thenReturn(CUSTOMER);
		when(rs.getInt(TradeRowMapper.VERSION_COLUMN)).thenReturn(0);
	}

}
