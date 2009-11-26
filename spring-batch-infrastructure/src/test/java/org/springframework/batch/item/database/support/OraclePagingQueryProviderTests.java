package org.springframework.batch.item.database.support;

import org.junit.Test;
import org.junit.Assert;

/**
 * @author Thomas Risberg
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
		Assert.assertEquals("", sql, s);
		pagingQueryProvider.setWhereClause("");
		String sql2 = "SELECT * FROM (SELECT id, name, age FROM foo ORDER BY id ASC) WHERE ROWNUM <= 100";
		String s2 = pagingQueryProvider.generateFirstPageQuery(pageSize);
		Assert.assertEquals("", sql2, s2);
	}

	@Test @Override
	public void testGenerateRemainingPagesQuery() {
		String sql = "SELECT * FROM (SELECT id, name, age FROM foo WHERE bar = 1 AND id > ? ORDER BY id ASC) WHERE ROWNUM <= 100";
		String s = pagingQueryProvider.generateRemainingPagesQuery(pageSize);
		Assert.assertEquals("", sql, s);
	}

	@Test @Override
	public void testGenerateJumpToItemQuery() {
		String sql = "SELECT SORT_KEY FROM ( SELECT id AS SORT_KEY, ROW_NUMBER() OVER (ORDER BY id ASC) AS ROW_NUMBER FROM foo WHERE bar = 1) WHERE ROW_NUMBER = 100";
		String s = pagingQueryProvider.generateJumpToItemQuery(145, pageSize);
		Assert.assertEquals("", sql, s);
	}
	
	@Test @Override
	public void testGenerateJumpToItemQueryForFirstPage() {
		String sql = "SELECT SORT_KEY FROM ( SELECT id AS SORT_KEY, ROW_NUMBER() OVER (ORDER BY id ASC) AS ROW_NUMBER FROM foo WHERE bar = 1) WHERE ROW_NUMBER = 1";
		String s = pagingQueryProvider.generateJumpToItemQuery(45, pageSize);
		Assert.assertEquals("", sql, s);
	}
}
