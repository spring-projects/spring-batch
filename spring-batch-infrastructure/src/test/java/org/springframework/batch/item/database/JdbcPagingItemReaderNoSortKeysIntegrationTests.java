/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.HashMap;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.database.support.HsqlPagingQueryProvider;
import org.springframework.batch.item.sample.Foo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests for {@link JdbcPagingItemReader} using saveState=false and no sort keys (process-indicator pattern).
 *
 * @author Jimmy Praet
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "data-source-context.xml")
@Transactional
public class JdbcPagingItemReaderNoSortKeysIntegrationTests {
	
	@Autowired
	private DataSource dataSource;
	
	private JdbcTemplate jdbcTemplate;
	
	@Before
	public void setUp() throws Exception {
		jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.update("update T_FOOS set value = 0");
	}
	
	@Test
	public void testReadFromJdbcPagingItemReaderWithProcessIndicatorPattern() throws Exception {
		JdbcPagingItemReader<Foo> reader = createItemReader(false);
		testReadWithProcessIndicator(reader);
	}

	@Test
	public void testReadFromJdbcPagingItemReaderWithProcessIndicatorPatternNamedParam() throws Exception {
		JdbcPagingItemReader<Foo> reader = createItemReader(true);
		testReadWithProcessIndicator(reader);
	}

	private void testReadWithProcessIndicator(JdbcPagingItemReader<Foo> reader) throws Exception {
		reader.open(new ExecutionContext());
		Foo item = null;
		
		for (int i = 1; i <= 5; i++) {
			item = reader.read();
			Assert.assertEquals("bar" + i, item.getName());
			markAsProcessed(item);
		}
		
		Assert.assertNull(reader.read());
	}

	private void markAsProcessed(Foo foo) {
		jdbcTemplate.update("update T_FOOS set value = 1 WHERE ID = ?", foo.getId());
	}

	private JdbcPagingItemReader<Foo> createItemReader(boolean namedParam) throws Exception {
		JdbcPagingItemReader<Foo> reader = new JdbcPagingItemReader<Foo>();
		reader.setDataSource(dataSource);
		HsqlPagingQueryProvider queryProvider = new HsqlPagingQueryProvider();
		queryProvider.setSelectClause("select ID, NAME, VALUE");
		queryProvider.setFromClause("from T_FOOS");
		queryProvider.setWhereClause("VALUE = " + (namedParam ? ":value" : "?"));
		queryProvider.setSortKeys(null);
		reader.setQueryProvider(queryProvider);
		reader.setRowMapper(
				new ParameterizedRowMapper<Foo>() {
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
		HashMap<String, Object> parameterValues = new HashMap<String, Object>();
		parameterValues.put(namedParam ? "value" : "1", 0);
		reader.setParameterValues(parameterValues);
		reader.setPageSize(3);
		reader.setSaveState(false);
		reader.afterPropertiesSet();
		return reader;
	}

}
