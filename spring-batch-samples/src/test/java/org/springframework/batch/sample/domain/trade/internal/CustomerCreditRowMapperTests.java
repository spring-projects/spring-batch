package org.springframework.batch.sample.domain.trade.internal;

import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.batch.sample.support.AbstractRowMapperTests;
import org.springframework.jdbc.core.RowMapper;

public class CustomerCreditRowMapperTests extends AbstractRowMapperTests {
	private static final int ID = 12;
	private static final String CUSTOMER = "Jozef Mak";
	private static final BigDecimal CREDIT = new BigDecimal("0.1");

	@Override
	protected Object expectedDomainObject() {
		CustomerCredit credit = new CustomerCredit();
		credit.setId(ID);
		credit.setCredit(CREDIT);
		credit.setName(CUSTOMER);
		return credit;
	}

	@Override
	protected RowMapper rowMapper() {
		return new CustomerCreditRowMapper();
	}

	@Override
	protected void setUpResultSetMock(ResultSet rs) throws SQLException {
		when(rs.getInt(CustomerCreditRowMapper.ID_COLUMN)).thenReturn(ID);
		when(rs.getString(CustomerCreditRowMapper.NAME_COLUMN)).thenReturn(CUSTOMER);
		when(rs.getBigDecimal(CustomerCreditRowMapper.CREDIT_COLUMN)).thenReturn(CREDIT);
	}
}
