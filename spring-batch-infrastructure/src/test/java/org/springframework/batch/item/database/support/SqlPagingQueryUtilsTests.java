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

package org.springframework.batch.item.database.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.database.Order;

/**
 * @author Thomas Risberg
 * @author Dave Syer
 * @author Michael Minella
 * @since 2.0
 */
class SqlPagingQueryUtilsTests {

	private final Map<String, Order> sortKeys = new LinkedHashMap<>(Map.of("ID", Order.ASCENDING));

	@Test
	void testGenerateLimitSqlQuery() {
		AbstractSqlPagingQueryProvider qp = new TestSqlPagingQueryProvider("FOO", "BAR", sortKeys);
		assertEquals("SELECT FOO FROM BAR ORDER BY ID ASC LIMIT 100",
				SqlPagingQueryUtils.generateLimitSqlQuery(qp, false, "LIMIT 100"));
		assertEquals("SELECT FOO FROM BAR WHERE ((ID > ?)) ORDER BY ID ASC LIMIT 100",
				SqlPagingQueryUtils.generateLimitSqlQuery(qp, true, "LIMIT 100"));
		qp.setWhereClause("BAZ IS NOT NULL");
		assertEquals("SELECT FOO FROM BAR WHERE BAZ IS NOT NULL ORDER BY ID ASC LIMIT 100",
				SqlPagingQueryUtils.generateLimitSqlQuery(qp, false, "LIMIT 100"));
		assertEquals("SELECT FOO FROM BAR WHERE (BAZ IS NOT NULL) AND ((ID > ?)) ORDER BY ID ASC LIMIT 100",
				SqlPagingQueryUtils.generateLimitSqlQuery(qp, true, "LIMIT 100"));
	}

	@Test
	void testGenerateTopSqlQuery() {
		AbstractSqlPagingQueryProvider qp = new TestSqlPagingQueryProvider("FOO", "BAR", sortKeys);
		assertEquals("SELECT TOP 100 FOO FROM BAR ORDER BY ID ASC",
				SqlPagingQueryUtils.generateTopSqlQuery(qp, false, "TOP 100"));
		assertEquals("SELECT TOP 100 FOO FROM BAR WHERE ((ID > ?)) ORDER BY ID ASC",
				SqlPagingQueryUtils.generateTopSqlQuery(qp, true, "TOP 100"));
		qp.setWhereClause("BAZ IS NOT NULL");
		assertEquals("SELECT TOP 100 FOO FROM BAR WHERE BAZ IS NOT NULL ORDER BY ID ASC",
				SqlPagingQueryUtils.generateTopSqlQuery(qp, false, "TOP 100"));
		assertEquals("SELECT TOP 100 FOO FROM BAR WHERE (BAZ IS NOT NULL) AND ((ID > ?)) ORDER BY ID ASC",
				SqlPagingQueryUtils.generateTopSqlQuery(qp, true, "TOP 100"));
	}

	@Test
	void testGenerateRowNumSqlQuery() {
		AbstractSqlPagingQueryProvider qp = new TestSqlPagingQueryProvider("FOO", "BAR", sortKeys);
		assertEquals("SELECT * FROM (SELECT FOO FROM BAR ORDER BY ID ASC) WHERE ROWNUMBER <= 100",
				SqlPagingQueryUtils.generateRowNumSqlQuery(qp, false, "ROWNUMBER <= 100"));
		assertEquals("SELECT * FROM (SELECT FOO FROM BAR ORDER BY ID ASC) WHERE ROWNUMBER <= 100 AND ((ID > ?))",
				SqlPagingQueryUtils.generateRowNumSqlQuery(qp, true, "ROWNUMBER <= 100"));
		qp.setWhereClause("BAZ IS NOT NULL");
		assertEquals("SELECT * FROM (SELECT FOO FROM BAR WHERE BAZ IS NOT NULL ORDER BY ID ASC) WHERE ROWNUMBER <= 100",
				SqlPagingQueryUtils.generateRowNumSqlQuery(qp, false, "ROWNUMBER <= 100"));
		assertEquals(
				"SELECT * FROM (SELECT FOO FROM BAR WHERE BAZ IS NOT NULL ORDER BY ID ASC) WHERE ROWNUMBER <= 100 AND ((ID > ?))",
				SqlPagingQueryUtils.generateRowNumSqlQuery(qp, true, "ROWNUMBER <= 100"));
	}

	@Test
	void testGenerateTopSqlQueryDescending() {
		sortKeys.put("ID", Order.DESCENDING);
		AbstractSqlPagingQueryProvider qp = new TestSqlPagingQueryProvider("FOO", "BAR", sortKeys);
		assertEquals("SELECT TOP 100 FOO FROM BAR ORDER BY ID DESC",
				SqlPagingQueryUtils.generateTopSqlQuery(qp, false, "TOP 100"));
		assertEquals("SELECT TOP 100 FOO FROM BAR WHERE ((ID < ?)) ORDER BY ID DESC",
				SqlPagingQueryUtils.generateTopSqlQuery(qp, true, "TOP 100"));
		qp.setWhereClause("BAZ IS NOT NULL");
		assertEquals("SELECT TOP 100 FOO FROM BAR WHERE BAZ IS NOT NULL ORDER BY ID DESC",
				SqlPagingQueryUtils.generateTopSqlQuery(qp, false, "TOP 100"));
		assertEquals("SELECT TOP 100 FOO FROM BAR WHERE (BAZ IS NOT NULL) AND ((ID < ?)) ORDER BY ID DESC",
				SqlPagingQueryUtils.generateTopSqlQuery(qp, true, "TOP 100"));
	}

	@Test
	void testGenerateRowNumSqlQueryDescending() {
		sortKeys.put("ID", Order.DESCENDING);
		AbstractSqlPagingQueryProvider qp = new TestSqlPagingQueryProvider("FOO", "BAR", sortKeys);
		assertEquals("SELECT * FROM (SELECT FOO FROM BAR ORDER BY ID DESC) WHERE ROWNUMBER <= 100",
				SqlPagingQueryUtils.generateRowNumSqlQuery(qp, false, "ROWNUMBER <= 100"));
		assertEquals("SELECT * FROM (SELECT FOO FROM BAR ORDER BY ID DESC) WHERE ROWNUMBER <= 100 AND ((ID < ?))",
				SqlPagingQueryUtils.generateRowNumSqlQuery(qp, true, "ROWNUMBER <= 100"));
		qp.setWhereClause("BAZ IS NOT NULL");
		assertEquals(
				"SELECT * FROM (SELECT FOO FROM BAR WHERE BAZ IS NOT NULL ORDER BY ID DESC) WHERE ROWNUMBER <= 100",
				SqlPagingQueryUtils.generateRowNumSqlQuery(qp, false, "ROWNUMBER <= 100"));
		assertEquals(
				"SELECT * FROM (SELECT FOO FROM BAR WHERE BAZ IS NOT NULL ORDER BY ID DESC) WHERE ROWNUMBER <= 100 AND ((ID < ?))",
				SqlPagingQueryUtils.generateRowNumSqlQuery(qp, true, "ROWNUMBER <= 100"));
	}

	private static class TestSqlPagingQueryProvider extends AbstractSqlPagingQueryProvider {

		public TestSqlPagingQueryProvider(String select, String from, Map<String, Order> sortKeys) {
			setSelectClause(select);
			setFromClause(from);
			setSortKeys(sortKeys);
		}

		@Override
		public @Nullable String generateFirstPageQuery(int pageSize) {
			return null;
		}

		@Override
		public @Nullable String generateRemainingPagesQuery(int pageSize) {
			return null;
		}

	}

}
