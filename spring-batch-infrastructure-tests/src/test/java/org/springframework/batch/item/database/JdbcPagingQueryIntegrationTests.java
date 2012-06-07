/*
 * Copyright 2006-2007 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;

/**
 * @author Dave Syer
 * @since 2.1
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "JdbcPagingItemReaderCommonTests-context.xml")
public class JdbcPagingQueryIntegrationTests {

	private static Log logger = LogFactory.getLog(JdbcPagingQueryIntegrationTests.class);

	@Autowired
	private DataSource dataSource;

	private int maxId;

	private SimpleJdbcTemplate jdbcTemplate;

	private int itemCount = 9;

	private int pageSize = 2;

	@Before
	public void init() {
		jdbcTemplate = new SimpleJdbcTemplate(dataSource);
		maxId = jdbcTemplate.queryForInt("SELECT MAX(ID) from T_FOOS");
		for (int i = itemCount; i > maxId; i--) {
			jdbcTemplate.update("INSERT into T_FOOS (ID,NAME,VALUE) values (?, ?, ?)", i, "foo" + i, i);
		}
		assertEquals(itemCount, SimpleJdbcTestUtils.countRowsInTable(jdbcTemplate, "T_FOOS"));
	}

	@After
	public void destroy() {
		jdbcTemplate.update("DELETE from T_FOOS where ID>?", maxId);
	}

	@Test
	public void testQueryFromStart() throws Exception {

		PagingQueryProvider queryProvider = getPagingQueryProvider();

		int total = SimpleJdbcTestUtils.countRowsInTable(jdbcTemplate, "T_FOOS");
		assertTrue(total > pageSize);
		int pages = total / pageSize;

		int count = 0;

		List<Map<String, Object>> list = jdbcTemplate.queryForList(queryProvider.generateFirstPageQuery(pageSize));
		logger.debug("First page result: " + list);
		assertEquals(pageSize, list.size());
		count += pageSize;
		Object oldValue = -1L;

		while (count < pages * pageSize) {
			Object startAfterValue = list.get(pageSize - 1).get(queryProvider.getSortKey());
			assertNotSame(oldValue, startAfterValue);
			list = jdbcTemplate.queryForList(queryProvider.generateRemainingPagesQuery(pageSize), startAfterValue);
			assertEquals(pageSize, list.size());
			count += pageSize;
			oldValue = startAfterValue;
		}

		if (count < total) {
			Object startAfterValue = list.get(pageSize - 1).get(queryProvider.getSortKey());
			list = jdbcTemplate.queryForList(queryProvider.generateRemainingPagesQuery(pageSize), startAfterValue);
			assertEquals(total - pages * pageSize, list.size());
			count += list.size();
		}

		assertEquals(total, count);

	}

	@Test
	public void testJumpToItem() throws Exception {

		PagingQueryProvider queryProvider = getPagingQueryProvider();

		int minId = jdbcTemplate.queryForInt("SELECT MIN(VALUE) FROM T_FOOS");

		String query = queryProvider.generateJumpToItemQuery(pageSize, pageSize);
		List<Map<String, Object>> list = jdbcTemplate.queryForList(query);
		logger.debug("Jump to page result: " + list);
		assertEquals(1, list.size());
		System.err.println(list);
		String expected = "[{sort_key=" + (minId + pageSize - 1);
		assertEquals(expected, list.toString().toLowerCase().substring(0, expected.length()));
		Object startAfterValue = list.get(0).entrySet().iterator().next().getValue();
		list = jdbcTemplate.queryForList(queryProvider.generateRemainingPagesQuery(pageSize), startAfterValue);
		assertEquals(pageSize, list.size());
		expected = "[{id=" + (minId + pageSize);
		// assertEquals(expected, list.toString().toLowerCase().substring(0, expected.length()));

	}

	protected PagingQueryProvider getPagingQueryProvider() throws Exception {

		SqlPagingQueryProviderFactoryBean factory = new SqlPagingQueryProviderFactoryBean();
		factory.setDataSource(dataSource);
		factory.setSelectClause("select ID, NAME, VALUE");
		factory.setFromClause("from T_FOOS");
		factory.setSortKey("VALUE");
		return (PagingQueryProvider) factory.getObject();

	}

}
