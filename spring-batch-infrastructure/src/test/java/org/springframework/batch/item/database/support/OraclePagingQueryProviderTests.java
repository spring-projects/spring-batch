/*
 * Copyright 2012 the original author or authors.
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

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;

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
	
	@Test
	public void testJumpToItemQueryForJobExecutionsFromBatchAdmin() {
		String sql = "SELECT JOB_EXECUTION_ID FROM (SELECT JOB_EXECUTION_ID, ROWNUM as TMP_ROW_NUM FROM (SELECT JOB_EXECUTION_ID FROM BATCH_JOB_EXECUTION E, BATCH_JOB_INSTANCE I WHERE E.JOB_INSTANCE_ID=I.JOB_INSTANCE_ID ORDER BY E.JOB_EXECUTION_ID DESC)) WHERE TMP_ROW_NUM = 20";

		String result = oraclePagingQueryProviderForJobExecutions().generateJumpToItemQuery(20, 20);

		assertEquals(sql, result);
	}

	@Test
	public void testRemainingPagesQueryForJobExecutionsFromBatchAdmin() {
		String sql = "SELECT * FROM (SELECT E.JOB_EXECUTION_ID, E.START_TIME, E.END_TIME, E.STATUS, E.EXIT_CODE, E.EXIT_MESSAGE, E.CREATE_TIME, E.LAST_UPDATED, E.VERSION, I.JOB_INSTANCE_ID, I.JOB_NAME FROM BATCH_JOB_EXECUTION E, BATCH_JOB_INSTANCE I WHERE E.JOB_INSTANCE_ID=I.JOB_INSTANCE_ID ORDER BY E.JOB_EXECUTION_ID DESC) WHERE ROWNUM <= 20 AND ((JOB_EXECUTION_ID < ?))";
	
		String result = oraclePagingQueryProviderForJobExecutions().generateRemainingPagesQuery(20);

		assertEquals(sql, result);
	}

	private PagingQueryProvider oraclePagingQueryProviderForJobExecutions() {
		OraclePagingQueryProvider pagingQueryProvider = new OraclePagingQueryProvider();
		pagingQueryProvider.setFromClause("BATCH_JOB_EXECUTION E, BATCH_JOB_INSTANCE I");
		pagingQueryProvider.setSelectClause("E.JOB_EXECUTION_ID, E.START_TIME, E.END_TIME, E.STATUS, E.EXIT_CODE, E.EXIT_MESSAGE, "
				+ "E.CREATE_TIME, E.LAST_UPDATED, E.VERSION, I.JOB_INSTANCE_ID, I.JOB_NAME");
		Map<String, Order> sortKeys = new HashMap<String, Order>();
		sortKeys.put("E.JOB_EXECUTION_ID", Order.DESCENDING);
		pagingQueryProvider.setSortKeys(sortKeys);
		pagingQueryProvider.setWhereClause("E.JOB_INSTANCE_ID=I.JOB_INSTANCE_ID");
		return pagingQueryProvider;
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
