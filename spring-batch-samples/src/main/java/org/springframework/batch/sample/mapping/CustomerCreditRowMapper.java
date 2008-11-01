package org.springframework.batch.sample.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.batch.sample.domain.CustomerCredit;
import org.springframework.jdbc.core.RowMapper;

public class CustomerCreditRowMapper implements RowMapper {
	
	public static final String ID_COLUMN = "id";
	public static final String NAME_COLUMN = "name";
	public static final String CREDIT_COLUMN = "credit";

	public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
        CustomerCredit customerCredit = new CustomerCredit();

        customerCredit.setId(rs.getInt(ID_COLUMN));
        customerCredit.setName(rs.getString(NAME_COLUMN));
        customerCredit.setCredit(rs.getBigDecimal(CREDIT_COLUMN));

        return customerCredit;
	}

}
