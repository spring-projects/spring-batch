/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.support.HsqlPagingQueryProvider;
import org.springframework.batch.item.sample.Foo;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.ContextConfiguration;
/**
 * Tests for {@link JpaPagingItemReader} where multiple tables are involved, requiring the sort key to 
 * be given table-qualified.
 *
 * @author Thomas Risberg
 * @author Michael Minella
 * @author Bridger Howell
 */
@ContextConfiguration(inheritLocations = false)
public class JdbcPagingItemReaderMultiTableTests extends AbstractGenericDataSourceItemReaderIntegrationTests {

	@Override
	protected ItemReader<Foo> createItemReader() throws Exception {
		JdbcPagingItemReader<Foo> inputSource = new JdbcPagingItemReader<Foo>();
		inputSource.setDataSource(dataSource);
		HsqlPagingQueryProvider queryProvider = new HsqlPagingQueryProvider();
		queryProvider.setSelectClause("select T_FOOS.ID, T_FOOS.NAME, T_FOOS.VALUE");
		queryProvider.setFromClause("from T_FOOS, T_BARS");
		queryProvider.setWhereClause("where T_BARS.ID = 1");
		Map<String, Order> sortKeys = new LinkedHashMap<String, Order>();
		sortKeys.put("T_FOOS.ID", Order.ASCENDING);
		queryProvider.setSortKeys(sortKeys);
		inputSource.setQueryProvider(queryProvider);
		inputSource.setRowMapper(
				new RowMapper<Foo>() {
					@Override
					public Foo mapRow(ResultSet rs, int i) throws SQLException {
						Foo foo = new Foo();
						foo.setId(rs.getInt(1));
						foo.setName(rs.getString(2));
						foo.setValue(rs.getInt(3));
						return foo;
					}
				}
		);
		inputSource.setPageSize(3);
		inputSource.afterPropertiesSet();
		inputSource.setSaveState(true);

		return inputSource;
	}
}
