package org.springframework.batch.sample.domain.trade.internal;

import static org.easymock.EasyMock.*;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.batch.sample.support.AbstractRowMapperTests;
import org.springframework.jdbc.core.RowMapper;

public class CustomerCreditRowMapperTests extends AbstractRowMapperTests {

	/**
	 * 
	 */
	private static final int ID = 12;
	private static final String CUSTOMER = "Jozef Mak";
	private static final BigDecimal CREDIT = new BigDecimal(0.1);

	protected Object expectedDomainObject() {
		CustomerCredit credit = new CustomerCredit();
		credit.setId(ID);
		credit.setCredit(CREDIT);
		credit.setName(CUSTOMER);
		return credit;
	}

	protected RowMapper rowMapper() {
		return new CustomerCreditRowMapper();
	}

	protected void setUpResultSetMock(ResultSet rs) throws SQLException {
		expect(rs.getInt(CustomerCreditRowMapper.ID_COLUMN)).andReturn(ID);
		expect(rs.getString(CustomerCreditRowMapper.NAME_COLUMN)).andReturn(CUSTOMER);
		expect(rs.getBigDecimal(CustomerCreditRowMapper.CREDIT_COLUMN)).andReturn(CREDIT);
	}

}
