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
package org.springframework.batch.infrastructure.item.database;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Disabled;

import org.springframework.batch.infrastructure.item.database.support.HsqlPagingQueryProvider;
import org.springframework.batch.infrastructure.item.sample.Foo;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Dave Syer
 * @author Thomas Risberg
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 */
@SpringJUnitConfig(
		locations = "/org/springframework/batch/infrastructure/item/database/JdbcPagingItemReaderParameterTests-context.xml")
@Disabled("This test fails when integration tests are skipped..") // FIXME make this test
																	// independent of
																	// other
																	// tests
class JdbcPagingItemReaderNamedParameterTests extends AbstractJdbcPagingItemReaderParameterTests {

	@Override
	protected AbstractPagingItemReader<Foo> getItemReader() throws Exception {
		HsqlPagingQueryProvider queryProvider = new HsqlPagingQueryProvider();
		queryProvider.setSelectClause("select ID, NAME, VALUE");
		queryProvider.setFromClause("from T_FOOS");
		queryProvider.setWhereClause("where VALUE >= :limit");
		JdbcPagingItemReader<Foo> reader = new JdbcPagingItemReader<>(dataSource, queryProvider);
		Map<String, Order> sortKeys = new LinkedHashMap<>();
		sortKeys.put("ID", Order.ASCENDING);
		queryProvider.setSortKeys(sortKeys);
		reader.setParameterValues(Collections.<String, Object>singletonMap("limit", 2));
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
	protected String getName() {
		return "JdbcPagingItemReader";
	}

}
