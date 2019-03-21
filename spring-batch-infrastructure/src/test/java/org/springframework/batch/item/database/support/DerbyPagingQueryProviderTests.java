/*
 * Copyright 2006-2012 the original author or authors.
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.batch.item.database.Order;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

/**
 * @author Thomas Risberg
 * @author Michael Minella
 * @author Will Schipp
 */
public class DerbyPagingQueryProviderTests extends AbstractSqlPagingQueryProviderTests {

	public DerbyPagingQueryProviderTests() {
		pagingQueryProvider = new DerbyPagingQueryProvider();
	}
	
	@Test
	public void testInit() throws Exception {
		DataSource ds = mock(DataSource.class);
		Connection con = mock(Connection.class);
		DatabaseMetaData dmd = mock(DatabaseMetaData.class);
		when(dmd.getDatabaseProductVersion()).thenReturn("10.4.1.3");
		when(con.getMetaData()).thenReturn(dmd);
		when(ds.getConnection()).thenReturn(con);
		pagingQueryProvider.init(ds);
	}

	@Test
	public void testInitWithRecentVersion() throws Exception {
		DataSource ds = mock(DataSource.class);
		Connection con = mock(Connection.class);
		DatabaseMetaData dmd = mock(DatabaseMetaData.class);
		when(dmd.getDatabaseProductVersion()).thenReturn("10.10.1.1");
		when(con.getMetaData()).thenReturn(dmd);
		when(ds.getConnection()).thenReturn(con);
		pagingQueryProvider.init(ds);
	}

	@Test
	public void testInitWithUnsupportedVersion() throws Exception {
		DataSource ds = mock(DataSource.class);
		Connection con = mock(Connection.class);
		DatabaseMetaData dmd = mock(DatabaseMetaData.class);
		when(dmd.getDatabaseProductVersion()).thenReturn("10.2.9.9");
		when(con.getMetaData()).thenReturn(dmd);
		when(ds.getConnection()).thenReturn(con);
		try {
			pagingQueryProvider.init(ds);
			fail();
		}
		catch (InvalidDataAccessResourceUsageException e) {
			// expected
		}
	}

	@Test
	@Override
	public void testGenerateFirstPageQuery() {
		String sql = "SELECT * FROM ( SELECT TMP_ORDERED.*, ROW_NUMBER() OVER () AS ROW_NUMBER FROM (SELECT id, name, age FROM foo WHERE bar = 1 ) AS TMP_ORDERED) AS TMP_SUB WHERE TMP_SUB.ROW_NUMBER <= 100 ORDER BY id ASC";
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		Assert.assertEquals(sql, s);
	}

	@Test
	@Override
	public void testGenerateRemainingPagesQuery() {
		String sql = "SELECT * FROM ( SELECT TMP_ORDERED.*, ROW_NUMBER() OVER () AS ROW_NUMBER FROM (SELECT id, name, age FROM foo WHERE bar = 1 ) AS TMP_ORDERED) AS TMP_SUB WHERE TMP_SUB.ROW_NUMBER <= 100 AND ((id > ?)) ORDER BY id ASC";
		String s = pagingQueryProvider.generateRemainingPagesQuery(pageSize);
		Assert.assertEquals(sql, s);
	}

	@Test
	@Override
	public void testGenerateJumpToItemQuery() {
		String sql = "SELECT id FROM ( SELECT id, ROW_NUMBER() OVER () AS ROW_NUMBER FROM (SELECT id, name, age FROM foo WHERE bar = 1 ) AS TMP_ORDERED) AS TMP_SUB WHERE TMP_SUB.ROW_NUMBER = 100 ORDER BY id ASC";
		String s = pagingQueryProvider.generateJumpToItemQuery(145, pageSize);
		Assert.assertEquals(sql, s);
	}

	@Test
	@Override
	public void testGenerateJumpToItemQueryForFirstPage() {
		String sql = "SELECT id FROM ( SELECT id, ROW_NUMBER() OVER () AS ROW_NUMBER FROM (SELECT id, name, age FROM foo WHERE bar = 1 ) AS TMP_ORDERED) AS TMP_SUB WHERE TMP_SUB.ROW_NUMBER = 1 ORDER BY id ASC";
		String s = pagingQueryProvider.generateJumpToItemQuery(45, pageSize);
		Assert.assertEquals(sql, s);
	}

	/**
	 * Older versions of Derby don't allow order by in the sub select.  This should work with 10.6.1 and above.
	 */
	@Test
	@Override
	public void testQueryContainsSortKey() {
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize).toLowerCase();
		assertTrue("Wrong query: " + s, s.contains("id asc"));
	}

	/**
	 * Older versions of Derby don't allow order by in the sub select.  This should work with 10.6.1 and above.
	 */
	@Test
	@Override
	public void testQueryContainsSortKeyDesc() {
		pagingQueryProvider.getSortKeys().put("id", Order.DESCENDING);
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize).toLowerCase();
		assertTrue("Wrong query: " + s, s.contains("id desc"));
	}

	@Override
	@Test
	public void testGenerateFirstPageQueryWithGroupBy() {
		pagingQueryProvider.setGroupClause("dep");
		String sql = "SELECT * FROM ( SELECT TMP_ORDERED.*, ROW_NUMBER() OVER () AS ROW_NUMBER FROM (SELECT id, name, age FROM foo WHERE bar = 1 GROUP BY dep ) AS TMP_ORDERED) AS TMP_SUB WHERE TMP_SUB.ROW_NUMBER <= 100 ORDER BY id ASC";
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		assertEquals(sql, s);
	}

	@Override
	@Test
	public void testGenerateRemainingPagesQueryWithGroupBy() {
		pagingQueryProvider.setGroupClause("dep");
		String sql = "SELECT * FROM ( SELECT TMP_ORDERED.*, ROW_NUMBER() OVER () AS ROW_NUMBER FROM (SELECT id, name, age FROM foo WHERE bar = 1 GROUP BY dep ) AS TMP_ORDERED) AS TMP_SUB WHERE TMP_SUB.ROW_NUMBER <= 100 AND ((id > ?)) ORDER BY id ASC";
		String s = pagingQueryProvider.generateRemainingPagesQuery(pageSize);
		assertEquals(sql, s);
	}

	@Override
	@Test
	public void testGenerateJumpToItemQueryWithGroupBy() {
		pagingQueryProvider.setGroupClause("dep");
		String sql = "SELECT id FROM ( SELECT id, ROW_NUMBER() OVER () AS ROW_NUMBER FROM (SELECT id, name, age FROM foo WHERE bar = 1 GROUP BY dep ) AS TMP_ORDERED) AS TMP_SUB WHERE TMP_SUB.ROW_NUMBER = 100 ORDER BY id ASC";
		String s = pagingQueryProvider.generateJumpToItemQuery(145, pageSize);
		assertEquals(sql, s);
	}

	@Override
	@Test
	public void testGenerateJumpToItemQueryForFirstPageWithGroupBy() {
		pagingQueryProvider.setGroupClause("dep");
		String sql = "SELECT id FROM ( SELECT id, ROW_NUMBER() OVER () AS ROW_NUMBER FROM (SELECT id, name, age FROM foo WHERE bar = 1 GROUP BY dep ) AS TMP_ORDERED) AS TMP_SUB WHERE TMP_SUB.ROW_NUMBER = 1 ORDER BY id ASC";
		String s = pagingQueryProvider.generateJumpToItemQuery(45, pageSize);
		assertEquals(sql, s);
	}

	@Override
	public String getFirstPageSqlWithMultipleSortKeys() {
		return "SELECT * FROM ( SELECT TMP_ORDERED.*, ROW_NUMBER() OVER () AS ROW_NUMBER FROM (SELECT id, name, age FROM foo WHERE bar = 1 ) AS TMP_ORDERED) AS TMP_SUB WHERE TMP_SUB.ROW_NUMBER <= 100 ORDER BY name ASC, id DESC";
	}

	@Override
	public String getRemainingSqlWithMultipleSortKeys() {
		return "SELECT * FROM ( SELECT TMP_ORDERED.*, ROW_NUMBER() OVER () AS ROW_NUMBER FROM (SELECT id, name, age FROM foo WHERE bar = 1 ) AS TMP_ORDERED) AS TMP_SUB WHERE TMP_SUB.ROW_NUMBER <= 100 AND ((name > ?) OR (name = ? AND id < ?)) ORDER BY name ASC, id DESC";
	}

	@Override
	public String getJumpToItemQueryWithMultipleSortKeys() {
		return "SELECT name, id FROM ( SELECT name, id, ROW_NUMBER() OVER () AS ROW_NUMBER FROM (SELECT id, name, age FROM foo WHERE bar = 1 ) AS TMP_ORDERED) AS TMP_SUB WHERE TMP_SUB.ROW_NUMBER = 100 ORDER BY name ASC, id DESC";
	}

	@Override
	public String getJumpToItemQueryForFirstPageWithMultipleSortKeys() {
		return "SELECT name, id FROM ( SELECT name, id, ROW_NUMBER() OVER () AS ROW_NUMBER FROM (SELECT id, name, age FROM foo WHERE bar = 1 ) AS TMP_ORDERED) AS TMP_SUB WHERE TMP_SUB.ROW_NUMBER = 1 ORDER BY name ASC, id DESC";
	}
}
