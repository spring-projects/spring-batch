/*
 * Copyright 2006-2021 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.batch.item.sample.Foo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.jdbc.JdbcTestUtils;

/**
 * @author Dave Syer
 * @author Michael Minella
 * @since 2.1
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "JdbcPagingItemReaderCommonTests-context.xml")
public class JdbcPagingRestartIntegrationTests {

	private static Log logger = LogFactory.getLog(JdbcPagingRestartIntegrationTests.class);

	@Autowired
	private DataSource dataSource;

	private int maxId;

	private JdbcTemplate jdbcTemplate;

	private int itemCount = 9;

	private int pageSize = 2;

	@Before
	public void init() {
		jdbcTemplate = new JdbcTemplate(dataSource);
		maxId = jdbcTemplate.queryForObject("SELECT MAX(ID) from T_FOOS", Integer.class);
		for (int i = itemCount; i > maxId; i--) {
			jdbcTemplate.update("INSERT into T_FOOS (ID,NAME,VALUE) values (?, ?, ?)", i, "foo" + i, i);
		}

		assertEquals(itemCount, JdbcTestUtils.countRowsInTable(jdbcTemplate, "T_FOOS"));
	}

	@After
	public void destroy() {
		jdbcTemplate.update("DELETE from T_FOOS where ID>?", maxId);
	}

	@Test
	@Ignore //FIXME
	public void testReaderFromStart() throws Exception {

		ItemReader<Foo> reader = getItemReader();

		int total = JdbcTestUtils.countRowsInTable(jdbcTemplate, "T_FOOS");

		ExecutionContext executionContext = new ExecutionContext();
		((ItemStream) reader).open(executionContext);

		for (int i = 0; i < total; i++) {
			Foo item = reader.read();
			logger.debug("Item: " + item);
			assertNotNull(item);
		}

		Foo item = reader.read();
		logger.debug("Item: " + item);
		assertNull(item);

	}

	@Test
	@Ignore //FIXME
	public void testReaderOnRestart() throws Exception {

		ItemReader<Foo> reader = getItemReader();

		int total = JdbcTestUtils.countRowsInTable(jdbcTemplate, "T_FOOS");
		int count = (total / pageSize) * pageSize;
		int pagesToRead = Math.min(3, total/pageSize);
		if (count >= pagesToRead*pageSize) {
			count -= pagesToRead*pageSize;
		}

		ExecutionContext executionContext = new ExecutionContext();
		executionContext.putInt("JdbcPagingItemReader.read.count", count);
		// Assume the primary keys are in order

		List<Map<String, Object>> ids = jdbcTemplate
				.queryForList("SELECT ID,NAME FROM T_FOOS ORDER BY ID ASC");
		logger.debug("Ids: "+ids);
		int startAfterValue = Integer.parseInt(ids.get(count - 1).get("ID").toString());
		logger.debug("Start after: " + startAfterValue);
		Map<String, Object> startAfterValues = new LinkedHashMap<>();
		startAfterValues.put("ID", startAfterValue);
		executionContext.put("JdbcPagingItemReader.start.after", startAfterValues);
		((ItemStream) reader).open(executionContext);

		for (int i = count; i < total; i++) {
			Foo item = reader.read();
			logger.debug("Item: " + item);
			assertNotNull(item);
		}

		Foo item = reader.read();
		logger.debug("Item: " + item);
		assertNull(item);

	}

	protected ItemReader<Foo> getItemReader() throws Exception {

		JdbcPagingItemReader<Foo> reader = new JdbcPagingItemReader<>();
		reader.setDataSource(dataSource);
		SqlPagingQueryProviderFactoryBean factory = new SqlPagingQueryProviderFactoryBean();
		factory.setDataSource(dataSource);
		factory.setSelectClause("select ID, NAME, VALUE");
		factory.setFromClause("from T_FOOS");
		Map<String, Order> sortKeys = new LinkedHashMap<>();
		sortKeys.put("VALUE", Order.ASCENDING);
		factory.setSortKeys(sortKeys);
		reader.setQueryProvider(factory.getObject());
		reader.setRowMapper(new RowMapper<Foo>() {
			@Override
			public Foo mapRow(ResultSet rs, int i) throws SQLException {
				Foo foo = new Foo();
				foo.setId(rs.getInt(1));
				foo.setName(rs.getString(2));
				foo.setValue(rs.getInt(3));
				return foo;
			}
		});
		reader.setPageSize(pageSize);
		reader.afterPropertiesSet();
		reader.setSaveState(true);

		return reader;
	}

}
