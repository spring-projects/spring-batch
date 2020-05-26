/*
 * Copyright 2012 the original author or authors.
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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Thomas Risberg
 * @author Michael Minella
 */
public class OraclePagingQueryProviderTests extends AbstractSqlPagingQueryProviderTests {

	public OraclePagingQueryProviderTests() {
		pagingQueryProvider = new OraclePagingQueryProvider();
	}

	@Test
	@Override
	public void testGenerateFirstPageQuery() {
		String sql = "SELECT * FROM (SELECT id, name, age FROM foo WHERE bar = 1 ORDER BY id ASC) WHERE ROWNUM <= 100";
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		assertEquals(sql, s);
		pagingQueryProvider.setWhereClause("");
		String sql2 = "SELECT * FROM (SELECT id, name, age FROM foo ORDER BY id ASC) WHERE ROWNUM <= 100";
		String s2 = pagingQueryProvider.generateFirstPageQuery(pageSize);
		assertEquals(sql2, s2);
	}

	@Test @Override
	public void testGenerateRemainingPagesQuery() {
		String sql = "SELECT * FROM (SELECT id, name, age FROM foo WHERE bar = 1 ORDER BY id ASC) WHERE ROWNUM <= 100 AND ((id > ?))";
		String s = pagingQueryProvider.generateRemainingPagesQuery(pageSize);
		assertEquals(sql, s);
	}

	@Test @Override
	public void testGenerateJumpToItemQuery() {
		String sql = "SELECT id FROM (SELECT id, ROWNUM as TMP_ROW_NUM FROM (SELECT id FROM foo WHERE bar = 1 ORDER BY id ASC)) WHERE TMP_ROW_NUM = 100";
		String s = pagingQueryProvider.generateJumpToItemQuery(145, pageSize);
		assertEquals(sql, s);
	}
	
	@Test @Override
	public void testGenerateJumpToItemQueryForFirstPage() {
		String sql = "SELECT id FROM (SELECT id, ROWNUM as TMP_ROW_NUM FROM (SELECT id FROM foo WHERE bar = 1 ORDER BY id ASC)) WHERE TMP_ROW_NUM = 1";
		String s = pagingQueryProvider.generateJumpToItemQuery(45, pageSize);
		assertEquals(sql, s);
	}

	@Override
	@Test
	public void testGenerateFirstPageQueryWithGroupBy() {
		pagingQueryProvider.setGroupClause("dep");
		String sql = "SELECT * FROM (SELECT id, name, age FROM foo WHERE bar = 1 GROUP BY dep ORDER BY id ASC) WHERE ROWNUM <= 100";
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		assertEquals(sql, s);
	}

	@Override
	@Test
	public void testGenerateRemainingPagesQueryWithGroupBy() {
		pagingQueryProvider.setGroupClause("dep");
		String sql = "SELECT * FROM (SELECT id, name, age FROM foo WHERE bar = 1 GROUP BY dep ORDER BY id ASC) WHERE ROWNUM <= 100 AND ((id > ?))";
		String s = pagingQueryProvider.generateRemainingPagesQuery(pageSize);
		assertEquals(sql, s);
	}

	@Override
	@Test
	public void testGenerateJumpToItemQueryWithGroupBy() {
		pagingQueryProvider.setGroupClause("dep");
		String sql = "SELECT id FROM (SELECT id, MIN(ROWNUM) as TMP_ROW_NUM FROM (SELECT id FROM foo WHERE bar = 1 GROUP BY dep ORDER BY id ASC)) WHERE TMP_ROW_NUM = 100";
		String s = pagingQueryProvider.generateJumpToItemQuery(145, pageSize);
		assertEquals(sql, s);
	}

	@Override
	@Test
	public void testGenerateJumpToItemQueryForFirstPageWithGroupBy() {
		pagingQueryProvider.setGroupClause("dep");
		String sql = "SELECT id FROM (SELECT id, MIN(ROWNUM) as TMP_ROW_NUM FROM (SELECT id FROM foo WHERE bar = 1 GROUP BY dep ORDER BY id ASC)) WHERE TMP_ROW_NUM = 1";
		String s = pagingQueryProvider.generateJumpToItemQuery(45, pageSize);
		assertEquals(sql, s);
	}

	@Override
	public String getFirstPageSqlWithMultipleSortKeys() {
		return "SELECT * FROM (SELECT id, name, age FROM foo WHERE bar = 1 ORDER BY name ASC, id DESC) WHERE ROWNUM <= 100";
	}

	@Override
	public String getRemainingSqlWithMultipleSortKeys() {
		return "SELECT * FROM (SELECT id, name, age FROM foo WHERE bar = 1 ORDER BY name ASC, id DESC) WHERE ROWNUM <= 100 AND ((name > ?) OR (name = ? AND id < ?))";
	}

	@Override
	public String getJumpToItemQueryWithMultipleSortKeys() {
		return "SELECT name, id FROM (SELECT name, id, ROWNUM as TMP_ROW_NUM FROM (SELECT name, id FROM foo WHERE bar = 1 ORDER BY name ASC, id DESC)) WHERE TMP_ROW_NUM = 100";
	}

	@Override
	public String getJumpToItemQueryForFirstPageWithMultipleSortKeys() {
		return "SELECT name, id FROM (SELECT name, id, ROWNUM as TMP_ROW_NUM FROM (SELECT name, id FROM foo WHERE bar = 1 ORDER BY name ASC, id DESC)) WHERE TMP_ROW_NUM = 1";
	}
}
