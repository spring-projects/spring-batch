/*
 * Copyright 2008-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.item.database;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.batch.item.sample.Foo;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

public class SingleKeyFooDao extends JdbcDaoSupport implements FooDao {

    @Override
	public Foo getFoo(Object key){

		RowMapper<Foo> fooMapper = new RowMapper<Foo>(){
            @Override
			public Foo mapRow(ResultSet rs, int rowNum) throws SQLException {
				Foo foo = new Foo();
				foo.setId(rs.getInt(1));
				foo.setName(rs.getString(2));
				foo.setValue(rs.getInt(3));
				return foo;
			}
		};

		return getJdbcTemplate().query("SELECT ID, NAME, VALUE from T_FOOS where ID = ?",
				new Object[] {key}, fooMapper).get(0);

	}
}
