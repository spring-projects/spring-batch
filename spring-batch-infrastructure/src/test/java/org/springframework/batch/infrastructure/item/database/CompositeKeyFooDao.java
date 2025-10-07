/*
 * Copyright 2006-2023 the original author or authors.
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
package org.springframework.batch.infrastructure.item.database;

import java.util.Map;

import org.springframework.batch.infrastructure.item.sample.Foo;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

import javax.sql.DataSource;

/**
 * @author Lucas Ward
 * @author Mahmoud Ben Hassine
 *
 */
public class CompositeKeyFooDao extends JdbcDaoSupport implements FooDao {

	public CompositeKeyFooDao(DataSource dataSource) {
		this.setDataSource(dataSource);
	}

	@Override
	public Foo getFoo(Object key) {

		Map<?, ?> keys = (Map<?, ?>) key;
		Object[] args = keys.values().toArray();

		RowMapper<Foo> fooMapper = (rs, rowNum) -> {
			Foo foo = new Foo();
			foo.setId(rs.getInt(1));
			foo.setName(rs.getString(2));
			foo.setValue(rs.getInt(3));
			return foo;
		};

		return getJdbcTemplate().query("SELECT ID, NAME, VALUE from T_FOOS where ID = ? and VALUE = ?", fooMapper, args)
			.get(0);
	}

}
