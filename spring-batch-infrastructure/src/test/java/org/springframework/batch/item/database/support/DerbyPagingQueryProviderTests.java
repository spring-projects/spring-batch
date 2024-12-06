/*
 * Copyright 2006-2024 the original author or authors.
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

/**
 * @author Thomas Risberg
 * @author Michael Minella
 * @author Will Schipp
 */
class DerbyPagingQueryProviderTests extends AbstractSqlPagingQueryProviderTests {

	DerbyPagingQueryProviderTests() {
		pagingQueryProvider = new DerbyPagingQueryProvider();
	}

	@Test
	@Override
	void testGenerateFirstPageQuery() {
		String sql = "SELECT id, name, age FROM foo WHERE bar = 1 ORDER BY id ASC FETCH FIRST 100 ROWS ONLY";
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		assertEquals(sql, s);
	}

	@Test
	@Override
	void testGenerateRemainingPagesQuery() {
		String sql = "SELECT id, name, age FROM foo WHERE (bar = 1) AND ((id > ?)) ORDER BY id ASC FETCH FIRST 100 ROWS ONLY";
		String s = pagingQueryProvider.generateRemainingPagesQuery(pageSize);
		assertEquals(sql, s);
	}

	@Test
	@Override
	void testGenerateFirstPageQueryWithGroupBy() {
		pagingQueryProvider.setGroupClause("dep");
		String sql = "SELECT id, name, age FROM foo WHERE bar = 1 GROUP BY dep ORDER BY id ASC FETCH FIRST 100 ROWS ONLY";
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		assertEquals(sql, s);
	}

	@Test
	@Override
	void testGenerateRemainingPagesQueryWithGroupBy() {
		pagingQueryProvider.setGroupClause("dep");
		String sql = "SELECT *  FROM (SELECT id, name, age FROM foo WHERE bar = 1 GROUP BY dep) AS MAIN_QRY WHERE ((id > ?)) ORDER BY id ASC FETCH FIRST 100 ROWS ONLY";
		String s = pagingQueryProvider.generateRemainingPagesQuery(pageSize);
		assertEquals(sql, s);
	}

	@Override
	String getFirstPageSqlWithMultipleSortKeys() {
		return "SELECT id, name, age FROM foo WHERE bar = 1 ORDER BY name ASC, id DESC FETCH FIRST 100 ROWS ONLY";
	}

	@Override
	String getRemainingSqlWithMultipleSortKeys() {
		return "SELECT id, name, age FROM foo WHERE (bar = 1) AND ((name > ?) OR (name = ? AND id < ?)) ORDER BY name ASC, id DESC FETCH FIRST 100 ROWS ONLY";
	}

}
