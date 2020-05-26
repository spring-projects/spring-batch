/*
 * Copyright 2012 the original author or authors.
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.item.database.support.HsqlPagingQueryProvider;
import org.springframework.batch.item.sample.Foo;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Dave Syer
 * @author Thomas Risberg
 * @author Michael Minella
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/org/springframework/batch/item/database/JdbcPagingItemReaderParameterTests-context.xml")
public class JdbcPagingItemReaderNamedParameterTests extends AbstractJdbcPagingItemReaderParameterTests {

	// force jumpToItemQuery in JdbcPagingItemReader.doJumpToPage(int)
	private static boolean forceJumpToItemQuery = false;

    @Override
	protected AbstractPagingItemReader<Foo> getItemReader() throws Exception {
		JdbcPagingItemReader<Foo> reader = new JdbcPagingItemReader<Foo>() {
			@Override
			protected void doJumpToPage(int itemIndex) {
				if (forceJumpToItemQuery) {
					ReflectionTestUtils.setField(this, "startAfterValues", null);
				}
				super.doJumpToPage(itemIndex);
			}
		};
		reader.setDataSource(dataSource);
		HsqlPagingQueryProvider queryProvider = new HsqlPagingQueryProvider();
		queryProvider.setSelectClause("select ID, NAME, VALUE");
		queryProvider.setFromClause("from T_FOOS");
		queryProvider.setWhereClause("where VALUE >= :limit");
		Map<String, Order> sortKeys = new LinkedHashMap<>();
		sortKeys.put("ID", Order.ASCENDING);
		queryProvider.setSortKeys(sortKeys);
		reader.setParameterValues(Collections.<String, Object>singletonMap("limit", 2));
		reader.setQueryProvider(queryProvider);
		reader.setRowMapper(
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
		reader.setPageSize(3);
		reader.afterPropertiesSet();
		reader.setSaveState(true);

		return reader;
		
	}
    
	@Test
	public void testReadAfterJumpSecondPageWithJumpToItemQuery() throws Exception {		
		try {
			forceJumpToItemQuery = true;
			super.testReadAfterJumpSecondPage();
		} finally {
			forceJumpToItemQuery = false;	
		}
	}
    
    @Override
    protected String getName() {
    	return "JdbcPagingItemReader";
    }

}
