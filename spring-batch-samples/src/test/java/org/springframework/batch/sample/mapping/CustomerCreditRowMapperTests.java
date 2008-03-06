package org.springframework.batch.sample.mapping;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.easymock.MockControl;
import org.springframework.batch.sample.domain.CustomerCredit;
import org.springframework.batch.sample.mapping.CustomerCreditRowMapper;
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

	protected void setUpResultSetMock(ResultSet rs, MockControl rsControl) throws SQLException {
		rs.getInt(CustomerCreditRowMapper.ID_COLUMN);
		rsControl.setReturnValue(ID);
		rs.getString(CustomerCreditRowMapper.NAME_COLUMN);
		rsControl.setReturnValue(CUSTOMER);
		rs.getBigDecimal(CustomerCreditRowMapper.CREDIT_COLUMN);
		rsControl.setReturnValue(CREDIT);
	}

}
