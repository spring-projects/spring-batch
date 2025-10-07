/*
 * Copyright 2006-2022 the original author or authors.
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.database.support.AbstractSqlPagingQueryProvider;
import org.springframework.batch.infrastructure.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS;

/**
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @since 2.1
 */
@SpringJUnitConfig(locations = "JdbcPagingItemReaderCommonTests-context.xml")
@DirtiesContext(classMode = BEFORE_CLASS)
class JdbcPagingQueryIntegrationTests {

	private static final Log logger = LogFactory.getLog(JdbcPagingQueryIntegrationTests.class);

	@Autowired
	private DataSource dataSource;

	private int maxId;

	private JdbcTemplate jdbcTemplate;

	private final int pageSize = 2;

	@BeforeEach
	void testInit() {
		jdbcTemplate = new JdbcTemplate(dataSource);
		String[] names = { "Foo", "Bar", "Baz", "Foo", "Bar", "Baz", "Foo", "Bar", "Baz" };
		String[] codes = { "A", "B", "A", "B", "B", "B", "A", "B", "A" };
		JdbcTestUtils.deleteFromTables(jdbcTemplate, "T_FOOS");
		for (int i = 0; i < names.length; i++) {
			jdbcTemplate.update("INSERT into T_FOOS (ID,NAME, CODE, VALUE) values (?, ?, ?, ?)", maxId, names[i],
					codes[i], i);
			maxId++;
		}
		assertEquals(9, JdbcTestUtils.countRowsInTable(jdbcTemplate, "T_FOOS"));
	}

	@AfterEach
	void destroy() {
		JdbcTestUtils.deleteFromTables(jdbcTemplate, "T_FOOS");
	}

	@Test
	void testQueryFromStart() throws Exception {

		PagingQueryProvider queryProvider = getPagingQueryProvider();

		int total = JdbcTestUtils.countRowsInTable(jdbcTemplate, "T_FOOS");
		assertTrue(total > pageSize);
		int pages = total / pageSize;

		int count = 0;

		List<Map<String, Object>> list = jdbcTemplate.queryForList(queryProvider.generateFirstPageQuery(pageSize));
		logger.debug("First page result: " + list);
		assertEquals(pageSize, list.size());
		count += pageSize;
		Map<String, Object> oldValues = null;

		while (count < pages * pageSize) {
			Map<String, Object> startAfterValues = getStartAfterValues(queryProvider, list);
			assertNotSame(oldValues, startAfterValues);
			list = jdbcTemplate.queryForList(queryProvider.generateRemainingPagesQuery(pageSize),
					getParameterList(null, startAfterValues).toArray());
			assertEquals(pageSize, list.size());
			count += pageSize;
			oldValues = startAfterValues;
		}

		if (count < total) {
			Map<String, Object> startAfterValues = getStartAfterValues(queryProvider, list);
			list = jdbcTemplate.queryForList(queryProvider.generateRemainingPagesQuery(pageSize),
					getParameterList(null, startAfterValues).toArray());
			assertEquals(total - pages * pageSize, list.size());
			count += list.size();
		}

		assertEquals(total, count);
	}

	@Test
	void testQueryFromStartWithGroupBy() throws Exception {
		AbstractSqlPagingQueryProvider queryProvider = (AbstractSqlPagingQueryProvider) getPagingQueryProvider();
		Map<String, Order> sortKeys = new LinkedHashMap<>();
		sortKeys.put("NAME", Order.ASCENDING);
		sortKeys.put("CODE", Order.DESCENDING);
		queryProvider.setSortKeys(sortKeys);
		queryProvider.setSelectClause("select NAME, CODE, sum(VALUE)");
		queryProvider.setGroupClause("NAME, CODE");

		int count = 0;
		int total = 5;

		List<Map<String, Object>> list = jdbcTemplate.queryForList(queryProvider.generateFirstPageQuery(pageSize));
		logger.debug("First page result: " + list);
		assertEquals(pageSize, list.size());
		count += pageSize;
		Map<String, Object> oldValues = null;

		while (count < total) {
			Map<String, Object> startAfterValues = getStartAfterValues(queryProvider, list);
			assertNotSame(oldValues, startAfterValues);
			list = jdbcTemplate.queryForList(queryProvider.generateRemainingPagesQuery(pageSize),
					getParameterList(null, startAfterValues).toArray());
			count += list.size();

			if (list.size() < pageSize) {
				assertEquals(1, list.size());
			}
			else {
				assertEquals(pageSize, list.size());
			}
			oldValues = startAfterValues;
		}

		assertEquals(total, count);
	}

	private Map<String, Object> getStartAfterValues(PagingQueryProvider queryProvider, List<Map<String, Object>> list) {
		Map<String, Object> startAfterValues = new LinkedHashMap<>();
		for (Map.Entry<String, Order> sortKey : queryProvider.getSortKeys().entrySet()) {
			startAfterValues.put(sortKey.getKey(), list.get(list.size() - 1).get(sortKey.getKey()));
		}
		return startAfterValues;
	}

	protected PagingQueryProvider getPagingQueryProvider() throws Exception {

		SqlPagingQueryProviderFactoryBean factory = new SqlPagingQueryProviderFactoryBean();
		factory.setDataSource(dataSource);
		factory.setSelectClause("select ID, NAME, VALUE");
		factory.setFromClause("from T_FOOS");
		Map<String, Order> sortKeys = new LinkedHashMap<>();
		sortKeys.put("VALUE", Order.ASCENDING);
		factory.setSortKeys(sortKeys);
		return factory.getObject();

	}

	private List<Object> getParameterList(Map<String, Object> values, Map<String, Object> sortKeyValue) {
		SortedMap<String, Object> sm = new TreeMap<>();
		if (values != null) {
			sm.putAll(values);
		}
		List<Object> parameterList = new ArrayList<>();
		parameterList.addAll(sm.values());
		if (sortKeyValue != null && sortKeyValue.size() > 0) {
			List<Map.Entry<String, Object>> keys = new ArrayList<>(sortKeyValue.entrySet());

			for (int i = 0; i < keys.size(); i++) {
				for (int j = 0; j < i; j++) {
					parameterList.add(keys.get(j).getValue());
				}

				parameterList.add(keys.get(i).getValue());
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Using parameterList:" + parameterList);
		}
		return parameterList;
	}

}
