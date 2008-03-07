package org.springframework.batch.item.database;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.batch.io.sample.domain.Foo;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

public class SingleKeyFooDao extends JdbcDaoSupport implements FooDao {

	public Foo getFoo(Object key){

		RowMapper fooMapper = new RowMapper(){
			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
				Foo foo = new Foo();
				foo.setId(rs.getInt(1));
				foo.setName(rs.getString(2));
				foo.setValue(rs.getInt(3));
				return foo;
			}
		};

		return (Foo)getJdbcTemplate().query("SELECT ID, NAME, VALUE from T_FOOS where ID = ?",
				new Object[] {key}, fooMapper).get(0);

	}
}
