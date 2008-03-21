package org.springframework.batch.core.resource;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;


public class FooRowMapper implements RowMapper {

		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {

			Foo foo = new Foo();
			foo.setId(rs.getInt(1));
			foo.setName(rs.getString(2));
			foo.setValue(rs.getInt(3));
			
			return foo;
		}
}
