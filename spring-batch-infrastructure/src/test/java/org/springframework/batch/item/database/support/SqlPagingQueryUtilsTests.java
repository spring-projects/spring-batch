/*
 * Copyright 2006-2008 the original author or authors.
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

package org.springframework.batch.item.database.support;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Thomas Risberg
 * @since 2.0
 */
public class SqlPagingQueryUtilsTests {

	@Test
	public void testGenerateLimitSqlQuery() {
		AbstractSqlPagingQueryProvider qp = new TestSqlPagingQueryProvider("FOO", "BAR", "ID");
		Assert.assertEquals("SELECT FOO FROM BAR ORDER BY ID ASC LIMIT 100", SqlPagingQueryUtils.generateLimitSqlQuery(
				qp, false, "LIMIT 100"));
		Assert.assertEquals("SELECT FOO FROM BAR WHERE ID > ? ORDER BY ID ASC LIMIT 100", SqlPagingQueryUtils.generateLimitSqlQuery(
				qp, true, "LIMIT 100"));
		qp.setWhereClause("BAZ IS NOT NULL");
		Assert.assertEquals("SELECT FOO FROM BAR WHERE BAZ IS NOT NULL ORDER BY ID ASC LIMIT 100", SqlPagingQueryUtils.generateLimitSqlQuery(
				qp, false, "LIMIT 100"));
		Assert.assertEquals("SELECT FOO FROM BAR WHERE BAZ IS NOT NULL AND ID > ? ORDER BY ID ASC LIMIT 100", SqlPagingQueryUtils.generateLimitSqlQuery(
				qp, true, "LIMIT 100"));
	}

	@Test
	public void testGenerateTopSqlQuery() {
		AbstractSqlPagingQueryProvider qp = new TestSqlPagingQueryProvider("FOO", "BAR", "ID");
		Assert.assertEquals("SELECT TOP 100 FOO FROM BAR ORDER BY ID ASC", SqlPagingQueryUtils.generateTopSqlQuery(
				qp, false, "TOP 100"));
		Assert.assertEquals("SELECT TOP 100 FOO FROM BAR WHERE ID > ? ORDER BY ID ASC", SqlPagingQueryUtils.generateTopSqlQuery(
				qp, true, "TOP 100"));
		qp.setWhereClause("BAZ IS NOT NULL");
		Assert.assertEquals("SELECT TOP 100 FOO FROM BAR WHERE BAZ IS NOT NULL ORDER BY ID ASC", SqlPagingQueryUtils.generateTopSqlQuery(
				qp, false, "TOP 100"));
		Assert.assertEquals("SELECT TOP 100 FOO FROM BAR WHERE BAZ IS NOT NULL AND ID > ? ORDER BY ID ASC", SqlPagingQueryUtils.generateTopSqlQuery(
				qp, true, "TOP 100"));
	}

	@Test
	public void testGenerateRowNumSqlQuery() {
		AbstractSqlPagingQueryProvider qp = new TestSqlPagingQueryProvider("FOO", "BAR", "ID");
		Assert.assertEquals("SELECT FOO FROM BAR WHERE ROWNUMBER <= 100 ORDER BY ID ASC", SqlPagingQueryUtils.generateRowNumSqlQuery(
				qp, false, "ROWNUMBER <= 100"));
		Assert.assertEquals("SELECT FOO FROM BAR WHERE ID > ? AND ROWNUMBER <= 100 ORDER BY ID ASC", SqlPagingQueryUtils.generateRowNumSqlQuery(
				qp, true, "ROWNUMBER <= 100"));
		qp.setWhereClause("BAZ IS NOT NULL");
		Assert.assertEquals("SELECT FOO FROM BAR WHERE BAZ IS NOT NULL AND ROWNUMBER <= 100 ORDER BY ID ASC", SqlPagingQueryUtils.generateRowNumSqlQuery(
				qp, false, "ROWNUMBER <= 100"));
		Assert.assertEquals("SELECT FOO FROM BAR WHERE BAZ IS NOT NULL AND ID > ? AND ROWNUMBER <= 100 ORDER BY ID ASC", SqlPagingQueryUtils.generateRowNumSqlQuery(
				qp, true, "ROWNUMBER <= 100"));
	}

	@Test
	public void testGenerateLimitJumpToQuery() {
		AbstractSqlPagingQueryProvider qp = new TestSqlPagingQueryProvider("FOO", "BAR", "ID");
		Assert.assertEquals("SELECT ID AS SORT_KEY FROM BAR ORDER BY ID ASC LIMIT 100, 1", SqlPagingQueryUtils.generateLimitJumpToQuery(
				qp, "LIMIT 100, 1"));
		qp.setWhereClause("BAZ IS NOT NULL");
		Assert.assertEquals("SELECT ID AS SORT_KEY FROM BAR WHERE BAZ IS NOT NULL ORDER BY ID ASC LIMIT 100, 1", SqlPagingQueryUtils.generateLimitJumpToQuery(
				qp, "LIMIT 100, 1"));
	}

	@Test
	public void testGenerateTopJumpToQuery() {
		AbstractSqlPagingQueryProvider qp = new TestSqlPagingQueryProvider("FOO", "BAR", "ID");
		Assert.assertEquals("SELECT TOP 100, 1 ID AS SORT_KEY FROM BAR ORDER BY ID ASC", SqlPagingQueryUtils.generateTopJumpToQuery(
				qp, "TOP 100, 1"));
		qp.setWhereClause("BAZ IS NOT NULL");
		Assert.assertEquals("SELECT TOP 100, 1 ID AS SORT_KEY FROM BAR WHERE BAZ IS NOT NULL ORDER BY ID ASC", SqlPagingQueryUtils.generateTopJumpToQuery(
				qp, "TOP 100, 1"));
	}


	private static class TestSqlPagingQueryProvider extends AbstractSqlPagingQueryProvider {

		public TestSqlPagingQueryProvider(String select, String from, String sortKey) {
			setSelectClause(select);
			setFromClause(from);
			setSortKey(sortKey);
		}

		public String generateFirstPageQuery(int pageSize) {
			return null;
		}

		public String generateRemainingPagesQuery(int pageSize) {
			return null;
		}

		public String generateJumpToItemQuery(int itemIndex, int pageSize) {
			return null;
		}

	}

}
