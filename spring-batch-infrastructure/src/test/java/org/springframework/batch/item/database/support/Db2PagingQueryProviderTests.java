package org.springframework.batch.item.database.support;

import org.junit.Test;
import org.junit.Assert;

/**
 * @author Thomas Risberg
 */
public class Db2PagingQueryProviderTests extends AbstractSqlPagingQueryProviderTests {

	public Db2PagingQueryProviderTests() {
		pagingQueryProvider = new Db2PagingQueryProvider();
	}

	@Test
	@Override
	public void testGenerateFirstPageQuery() {
		String sql = "SELECT id, name, age FROM foo WHERE bar = 1 FETCH FIRST 100 ROWS ONLY";
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		Assert.assertEquals("", sql, s);
	}

	@Test @Override
	public void testGenerateRemainingPagesQuery() {
		String sql = "SELECT id, name, age FROM foo WHERE id > ? AND bar = 1 FETCH FIRST 100 ROWS ONLY";
		String s = pagingQueryProvider.generateRemainingPagesQuery(pageSize);
		Assert.assertEquals("", sql, s);
	}

	@Test @Override
	public void testGenerateJumpToItemQuery() {
		String sql = "SELECT SORT_KEY FROM ( SELECT id AS SORT_KEY, ROW_NUMBER() OVER (ORDER BY id ASC) AS ROW_NUMBER FROM foo WHERE bar = 1) WHERE ROW_NUMBER = 100";
		String s = pagingQueryProvider.generateJumpToItemQuery(145, pageSize);
		Assert.assertEquals("", sql, s);
	}
}
