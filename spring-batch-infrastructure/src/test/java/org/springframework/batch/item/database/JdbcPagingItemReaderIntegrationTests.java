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

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.support.HsqlPagingQueryProvider;
import org.springframework.batch.item.sample.Foo;

/**
 * Tests for {@link JdbcPagingItemReader}.
 *
 * @author Thomas Risberg
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 */
class JdbcPagingItemReaderIntegrationTests extends AbstractGenericDataSourceItemReaderIntegrationTests {

	@Override
	protected ItemReader<Foo> createItemReader() throws Exception {

		JdbcPagingItemReader<Foo> itemReader = new JdbcPagingItemReader<>();
		itemReader.setDataSource(dataSource);
		HsqlPagingQueryProvider queryProvider = new HsqlPagingQueryProvider();
		queryProvider.setSelectClause("select ID, NAME, VALUE");
		queryProvider.setFromClause("from T_FOOS");
		Map<String, Order> sortKeys = new LinkedHashMap<>();
		sortKeys.put("ID", Order.ASCENDING);
		queryProvider.setSortKeys(sortKeys);
		itemReader.setQueryProvider(queryProvider);
		itemReader.setRowMapper((rs, i) -> {
			Foo foo = new Foo();
			foo.setId(rs.getInt(1));
			foo.setName(rs.getString(2));
			foo.setValue(rs.getInt(3));
			return foo;
		});
		itemReader.setPageSize(3);
		itemReader.afterPropertiesSet();
		itemReader.setSaveState(true);

		return itemReader;
	}

}
