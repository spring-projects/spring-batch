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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

/**
 * @author Thomas Risberg
 */
public class DerbyPagingQueryProviderTests extends AbstractSqlPagingQueryProviderTests {

	public DerbyPagingQueryProviderTests() {
		pagingQueryProvider = new DerbyPagingQueryProvider();
	}

	@Test
	public void testInit() throws Exception {
		DataSource ds = createMock(DataSource.class);
		Connection con = createMock(Connection.class);
		DatabaseMetaData dmd = createMock(DatabaseMetaData.class);
		expect(dmd.getDatabaseProductVersion()).andReturn("10.4.1.3");
		expect(con.getMetaData()).andReturn(dmd);
		expect(ds.getConnection()).andReturn(con);
		replay(dmd);
		replay(con);
		replay(ds);
		pagingQueryProvider.init(ds);
		verify(ds);
		verify(con);
		verify(dmd);
	}

	@Test
	public void testInitWithUnsupportedVErsion() throws Exception {
		DataSource ds = createMock(DataSource.class);
		Connection con = createMock(Connection.class);
		DatabaseMetaData dmd = createMock(DatabaseMetaData.class);
		expect(dmd.getDatabaseProductVersion()).andReturn("10.2.9.9");
		expect(con.getMetaData()).andReturn(dmd);
		expect(ds.getConnection()).andReturn(con);
		replay(dmd);
		replay(con);
		replay(ds);
		try {
			pagingQueryProvider.init(ds);
			fail();
		}
		catch (InvalidDataAccessResourceUsageException e) {
			// expected
		}
		verify(ds);
		verify(con);
		verify(dmd);
	}

	@Test
	@Override
	public void testGenerateFirstPageQuery() {
		String sql = "SELECT * FROM ( SELECT id, name, age, ROW_NUMBER() OVER () AS ROW_NUMBER FROM foo WHERE bar = 1) AS TMP_SUB WHERE ROW_NUMBER <= 100";
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		Assert.assertEquals("", sql, s);
	}

	@Test
	@Override
	public void testGenerateRemainingPagesQuery() {
		String sql = "SELECT * FROM ( SELECT id, name, age, ROW_NUMBER() OVER () AS ROW_NUMBER FROM foo WHERE bar = 1 AND id > ?) AS TMP_SUB WHERE ROW_NUMBER <= 100";
		String s = pagingQueryProvider.generateRemainingPagesQuery(pageSize);
		Assert.assertEquals("", sql, s);
	}

	@Test
	@Override
	public void testGenerateJumpToItemQuery() {
		String sql = "SELECT SORT_KEY FROM ( SELECT id AS SORT_KEY, ROW_NUMBER() OVER () AS ROW_NUMBER FROM foo WHERE bar = 1) AS TMP_SUB WHERE ROW_NUMBER = 100";
		String s = pagingQueryProvider.generateJumpToItemQuery(145, pageSize);
		Assert.assertEquals("", sql, s);
	}

	@Test
	@Override
	public void testGenerateJumpToItemQueryForFirstPage() {
		String sql = "SELECT SORT_KEY FROM ( SELECT id AS SORT_KEY, ROW_NUMBER() OVER () AS ROW_NUMBER FROM foo WHERE bar = 1) AS TMP_SUB WHERE ROW_NUMBER = 1";
		String s = pagingQueryProvider.generateJumpToItemQuery(45, pageSize);
		Assert.assertEquals("", sql, s);
	}

	/**
	 * Derby doesn't allow order by in the sub select (so it's pretty useless in
	 * general for paging)
	 */
	@Test
	@Override
	public void testQueryContainsSortKey() {
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize).toLowerCase();
		assertFalse("Wrong query: " + s, s.contains("id asc"));
	}

	/**
	 * Derby doesn't allow order by in the sub select
	 */
	@Test
	@Override
	public void testQueryContainsSortKeyDesc() {
		pagingQueryProvider.setAscending(false);
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize).toLowerCase();
		assertFalse("Wrong query: " + s, s.contains("id desc"));
	}

}
