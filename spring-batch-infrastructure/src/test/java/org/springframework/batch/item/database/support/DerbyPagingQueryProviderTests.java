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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.batch.item.database.Order;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

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
	void testInit() throws Exception {
		DataSource ds = mock(DataSource.class);
		Connection con = mock(Connection.class);
		DatabaseMetaData dmd = mock(DatabaseMetaData.class);
		when(dmd.getDatabaseProductVersion()).thenReturn("10.4.1.3");
		when(con.getMetaData()).thenReturn(dmd);
		when(ds.getConnection()).thenReturn(con);
		pagingQueryProvider.init(ds);
	}

	@Test
	void testInitWithRecentVersion() throws Exception {
		DataSource ds = mock(DataSource.class);
		Connection con = mock(Connection.class);
		DatabaseMetaData dmd = mock(DatabaseMetaData.class);
		when(dmd.getDatabaseProductVersion()).thenReturn("10.10.1.1");
		when(con.getMetaData()).thenReturn(dmd);
		when(ds.getConnection()).thenReturn(con);
		pagingQueryProvider.init(ds);
	}

	@Test
	void testInitWithUnsupportedVersion() throws Exception {
		DataSource ds = mock(DataSource.class);
		Connection con = mock(Connection.class);
		DatabaseMetaData dmd = mock(DatabaseMetaData.class);
		when(dmd.getDatabaseProductVersion()).thenReturn("10.2.9.9");
		when(con.getMetaData()).thenReturn(dmd);
		when(ds.getConnection()).thenReturn(con);
		assertThrows(InvalidDataAccessResourceUsageException.class, () -> pagingQueryProvider.init(ds));
	}

	@Test
	@Override
	void testGenerateFirstPageQuery() {
		String sql = "SELECT * FROM ( SELECT TMP_ORDERED.*, ROW_NUMBER() OVER () AS ROW_NUMBER FROM (SELECT id, name, age FROM foo WHERE bar = 1 ) AS TMP_ORDERED) AS TMP_SUB WHERE TMP_SUB.ROW_NUMBER <= 100 ORDER BY id ASC";
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		assertEquals(sql, s);
	}

	@Test
	@Override
	void testGenerateRemainingPagesQuery() {
		String sql = "SELECT * FROM ( SELECT TMP_ORDERED.*, ROW_NUMBER() OVER () AS ROW_NUMBER FROM (SELECT id, name, age FROM foo WHERE bar = 1 ) AS TMP_ORDERED) AS TMP_SUB WHERE TMP_SUB.ROW_NUMBER <= 100 AND ((id > ?)) ORDER BY id ASC";
		String s = pagingQueryProvider.generateRemainingPagesQuery(pageSize);
		assertEquals(sql, s);
	}

	/**
	 * Older versions of Derby don't allow order by in the sub select. This should work
	 * with 10.6.1 and above.
	 */
	@Test
	@Override
	void testQueryContainsSortKey() {
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize).toLowerCase();
		assertTrue(s.contains("id asc"), "Wrong query: " + s);
	}

	/**
	 * Older versions of Derby don't allow order by in the sub select. This should work
	 * with 10.6.1 and above.
	 */
	@Test
	@Override
	void testQueryContainsSortKeyDesc() {
		pagingQueryProvider.getSortKeys().put("id", Order.DESCENDING);
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize).toLowerCase();
		assertTrue(s.contains("id desc"), "Wrong query: " + s);
	}

	@Override
	@Test
	void testGenerateFirstPageQueryWithGroupBy() {
		pagingQueryProvider.setGroupClause("dep");
		String sql = "SELECT * FROM ( SELECT TMP_ORDERED.*, ROW_NUMBER() OVER () AS ROW_NUMBER FROM (SELECT id, name, age FROM foo WHERE bar = 1 GROUP BY dep ) AS TMP_ORDERED) AS TMP_SUB WHERE TMP_SUB.ROW_NUMBER <= 100 ORDER BY id ASC";
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		assertEquals(sql, s);
	}

	@Override
	@Test
	void testGenerateRemainingPagesQueryWithGroupBy() {
		pagingQueryProvider.setGroupClause("dep");
		String sql = "SELECT * FROM ( SELECT TMP_ORDERED.*, ROW_NUMBER() OVER () AS ROW_NUMBER FROM (SELECT id, name, age FROM foo WHERE bar = 1 GROUP BY dep ) AS TMP_ORDERED) AS TMP_SUB WHERE TMP_SUB.ROW_NUMBER <= 100 AND ((id > ?)) ORDER BY id ASC";
		String s = pagingQueryProvider.generateRemainingPagesQuery(pageSize);
		assertEquals(sql, s);
	}

	@Override
	String getFirstPageSqlWithMultipleSortKeys() {
		return "SELECT * FROM ( SELECT TMP_ORDERED.*, ROW_NUMBER() OVER () AS ROW_NUMBER FROM (SELECT id, name, age FROM foo WHERE bar = 1 ) AS TMP_ORDERED) AS TMP_SUB WHERE TMP_SUB.ROW_NUMBER <= 100 ORDER BY name ASC, id DESC";
	}

	@Override
	String getRemainingSqlWithMultipleSortKeys() {
		return "SELECT * FROM ( SELECT TMP_ORDERED.*, ROW_NUMBER() OVER () AS ROW_NUMBER FROM (SELECT id, name, age FROM foo WHERE bar = 1 ) AS TMP_ORDERED) AS TMP_SUB WHERE TMP_SUB.ROW_NUMBER <= 100 AND ((name > ?) OR (name = ? AND id < ?)) ORDER BY name ASC, id DESC";
	}

}
