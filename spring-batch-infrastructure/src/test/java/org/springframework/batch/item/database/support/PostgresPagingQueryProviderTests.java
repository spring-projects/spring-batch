/*
 * Copyright 2012-2025 the original author or authors.
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
package org.springframework.batch.item.database.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.batch.item.database.Order;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Thomas Risberg
 * @author Michael Minella
 * @author Kyeonghoon Lee
 */
class PostgresPagingQueryProviderTests extends AbstractSqlPagingQueryProviderTests {

	PostgresPagingQueryProviderTests() {
		pagingQueryProvider = new PostgresPagingQueryProvider();
	}

	@Test
	@Override
	void testGenerateFirstPageQuery() {
		String sql = "SELECT id, name, age FROM foo WHERE bar = 1 ORDER BY id ASC LIMIT 100";
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		assertEquals(sql, s);
	}

	@Test
	@Override
	void testGenerateRemainingPagesQuery() {
		String sql = "SELECT id, name, age FROM foo WHERE (bar = 1) AND ((id > ?)) ORDER BY id ASC LIMIT 100";
		String s = pagingQueryProvider.generateRemainingPagesQuery(pageSize);
		assertEquals(sql, s);
	}

	@Override
	@Test
	void testGenerateFirstPageQueryWithGroupBy() {
		pagingQueryProvider.setGroupClause("id, dep");
		String sql = "SELECT id, name, age FROM foo WHERE bar = 1 GROUP BY id, dep ORDER BY id ASC LIMIT 100";
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		assertEquals(sql, s);
	}

	@Override
	@Test
	void testGenerateRemainingPagesQueryWithGroupBy() {
		pagingQueryProvider.setGroupClause("id, dep");
		String sql = "SELECT *  FROM (SELECT id, name, age FROM foo WHERE bar = 1 GROUP BY id, dep) AS MAIN_QRY WHERE ((id > ?)) ORDER BY id ASC LIMIT 100";
		String s = pagingQueryProvider.generateRemainingPagesQuery(pageSize);
		assertEquals(sql, s);
	}

	@Test
	void testGenerateRemainingPagesQueryWithGroupByWithAlias() {
		pagingQueryProvider.setSelectClause("SELECT f.id, f.name, f.age");
		pagingQueryProvider.setFromClause("FROM foo f");
		pagingQueryProvider.setWhereClause("f.bar = 1");
		pagingQueryProvider.setGroupClause("f.id, f.dep");
		Map<String, Order> sortKeys = new LinkedHashMap<>();
		sortKeys.put("f.id", Order.ASCENDING);
		pagingQueryProvider.setSortKeys(sortKeys);

		String sql = "SELECT *  FROM (SELECT f.id, f.name, f.age FROM foo f WHERE f.bar = 1 GROUP BY f.id, f.dep) AS MAIN_QRY WHERE ((id > ?)) ORDER BY id ASC LIMIT "
				+ pageSize;
		String s = pagingQueryProvider.generateRemainingPagesQuery(pageSize);
		assertEquals(sql, s);
	}

	@Override
	String getFirstPageSqlWithMultipleSortKeys() {
		return "SELECT id, name, age FROM foo WHERE bar = 1 ORDER BY name ASC, id DESC LIMIT 100";
	}

	@Override
	String getRemainingSqlWithMultipleSortKeys() {
		return "SELECT id, name, age FROM foo WHERE (bar = 1) AND ((name > ?) OR (name = ? AND id < ?)) ORDER BY name ASC, id DESC LIMIT 100";
	}

}
