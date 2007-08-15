package org.springframework.batch.sample.mapping;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.easymock.MockControl;
import org.springframework.batch.sample.domain.Trade;
import org.springframework.batch.sample.mapping.TradeRowMapper;
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

	protected void setUpResultSetMock(ResultSet rs, MockControl rsControl) throws SQLException {
		rs.getString(TradeRowMapper.ISIN_COLUMN);
		rsControl.setReturnValue(ISIN);
		
		rs.getLong(TradeRowMapper.QUANTITY_COLUMN);
		rsControl.setReturnValue(QUANTITY);
		
		rs.getBigDecimal(TradeRowMapper.PRICE_COLUMN);
		rsControl.setReturnValue(PRICE);
		
		rs.getString(TradeRowMapper.CUSTOMER_COLUMN);
		rsControl.setReturnValue(CUSTOMER);
	}

}
