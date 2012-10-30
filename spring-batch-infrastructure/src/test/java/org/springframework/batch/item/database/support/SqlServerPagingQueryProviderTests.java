package org.springframework.batch.item.database.support;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Thomas Risberg
 * @author Michael Minella
 */
public class SqlServerPagingQueryProviderTests extends AbstractSqlPagingQueryProviderTests {

	public SqlServerPagingQueryProviderTests() {
		pagingQueryProvider = new SqlServerPagingQueryProvider();
	}

	@Test
	@Override
	public void testGenerateFirstPageQuery() {
		String sql = "SELECT TOP 100 id, name, age FROM foo WHERE bar = 1 ORDER BY id ASC";
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		assertEquals(sql, s);
	}

	@Test @Override
	public void testGenerateRemainingPagesQuery() {
		String sql = "SELECT TOP 100 id, name, age FROM foo WHERE bar = 1 AND id > ? ORDER BY id ASC";
		String s = pagingQueryProvider.generateRemainingPagesQuery(pageSize);
		assertEquals(sql, s);
	}

	@Test @Override
	public void testGenerateJumpToItemQuery() {
		String sql = "SELECT SORT_KEY FROM ( SELECT id AS SORT_KEY, ROW_NUMBER() OVER (ORDER BY id ASC) AS ROW_NUMBER FROM foo WHERE bar = 1) AS TMP_SUB WHERE TMP_SUB.ROW_NUMBER = 100";
		String s = pagingQueryProvider.generateJumpToItemQuery(145, pageSize);
		assertEquals(sql, s);
	}

	@Test @Override
	public void testGenerateJumpToItemQueryForFirstPage() {
		String sql = "SELECT SORT_KEY FROM ( SELECT id AS SORT_KEY, ROW_NUMBER() OVER (ORDER BY id ASC) AS ROW_NUMBER FROM foo WHERE bar = 1) AS TMP_SUB WHERE TMP_SUB.ROW_NUMBER = 1";
		String s = pagingQueryProvider.generateJumpToItemQuery(45, pageSize);
		assertEquals(sql, s);
	}

	@Override
	@Test
	public void testGenerateFirstPageQueryWithGroupBy() {
		pagingQueryProvider.setGroupClause("dep");
		String sql = "SELECT TOP 100 id, name, age FROM foo WHERE bar = 1 GROUP BY dep ORDER BY id ASC";
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		assertEquals(sql, s);
	}

	@Override
	@Test
	public void testGenerateRemainingPagesQueryWithGroupBy() {
		pagingQueryProvider.setGroupClause("dep");
		String sql = "SELECT TOP 100 id, name, age FROM foo WHERE bar = 1 AND id > ? GROUP BY dep ORDER BY id ASC";
		String s = pagingQueryProvider.generateRemainingPagesQuery(pageSize);
		assertEquals(sql, s);
	}

	@Override
	@Test
	public void testGenerateJumpToItemQueryWithGroupBy() {
		pagingQueryProvider.setGroupClause("dep");
		String sql = "SELECT SORT_KEY FROM ( SELECT id AS SORT_KEY, ROW_NUMBER() OVER (ORDER BY id ASC) AS ROW_NUMBER FROM foo WHERE bar = 1 GROUP BY dep) AS TMP_SUB WHERE TMP_SUB.ROW_NUMBER = 100";
		String s = pagingQueryProvider.generateJumpToItemQuery(145, pageSize);
		assertEquals(sql, s);
	}

	@Override
	@Test
	public void testGenerateJumpToItemQueryForFirstPageWithGroupBy() {
		pagingQueryProvider.setGroupClause("dep");
		String sql = "SELECT SORT_KEY FROM ( SELECT id AS SORT_KEY, ROW_NUMBER() OVER (ORDER BY id ASC) AS ROW_NUMBER FROM foo WHERE bar = 1 GROUP BY dep) AS TMP_SUB WHERE TMP_SUB.ROW_NUMBER = 1";
		String s = pagingQueryProvider.generateJumpToItemQuery(45, pageSize);
		assertEquals(sql, s);
	}
}
