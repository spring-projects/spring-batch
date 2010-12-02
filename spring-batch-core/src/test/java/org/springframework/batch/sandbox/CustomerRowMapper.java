package org.springframework.batch.sandbox;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * @author Josh Long
 */
public class CustomerRowMapper implements RowMapper  {
	public Object mapRow(ResultSet resultSet, int i) throws SQLException {
		return new Customer( resultSet.getLong( "ID") , resultSet.getString("FIRST_NAME") , resultSet.getString( "LAST_NAME") , resultSet.getString( "EMAIL")) ;
	}
}
