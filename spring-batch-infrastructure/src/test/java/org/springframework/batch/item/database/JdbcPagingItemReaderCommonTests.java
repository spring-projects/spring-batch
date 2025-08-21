/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.item.AbstractItemStreamItemReaderTests;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.support.HsqlPagingQueryProvider;
import org.springframework.batch.item.sample.Foo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS;

/**
 * @author Dave Syer
 * @author Thomas Risberg
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 */
@SpringJUnitConfig
@DirtiesContext(classMode = BEFORE_CLASS)
public class JdbcPagingItemReaderCommonTests extends AbstractItemStreamItemReaderTests {

	@Autowired
	private DataSource dataSource;

	@Override
	protected ItemReader<Foo> getItemReader() throws Exception {

		JdbcPagingItemReader<Foo> reader = new JdbcPagingItemReader<>();
		reader.setDataSource(dataSource);
		HsqlPagingQueryProvider queryProvider = new HsqlPagingQueryProvider();
		queryProvider.setSelectClause("select ID, NAME, VALUE");
		queryProvider.setFromClause("from T_FOOS");
		Map<String, Order> sortKeys = new LinkedHashMap<>();
		sortKeys.put("ID", Order.ASCENDING);
		queryProvider.setSortKeys(sortKeys);
		reader.setQueryProvider(queryProvider);
		reader.setRowMapper((rs, i) -> {
			Foo foo = new Foo();
			foo.setId(rs.getInt(1));
			foo.setName(rs.getString(2));
			foo.setValue(rs.getInt(3));
			return foo;
		});
		reader.setPageSize(3);
		reader.afterPropertiesSet();
		reader.setSaveState(true);

		return reader;
	}

	@Override
	protected void pointToEmptyInput(ItemReader<Foo> tested) throws Exception {
		JdbcPagingItemReader<Foo> reader = (JdbcPagingItemReader<Foo>) tested;
		reader.close();
		reader.setDataSource(dataSource);
		HsqlPagingQueryProvider queryProvider = new HsqlPagingQueryProvider();
		queryProvider.setSelectClause("select ID, NAME, VALUE");
		queryProvider.setFromClause("from T_FOOS");
		queryProvider.setWhereClause("where ID = -1");
		Map<String, Order> sortKeys = new LinkedHashMap<>();
		sortKeys.put("ID", Order.ASCENDING);
		queryProvider.setSortKeys(sortKeys);
		reader.setQueryProvider(queryProvider);
		reader.setPageSize(3);
		reader.afterPropertiesSet();
		reader.open(new ExecutionContext());
	}

}
