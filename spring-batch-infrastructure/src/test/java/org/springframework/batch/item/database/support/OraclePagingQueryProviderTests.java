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
		String sql = "SELECT * FROM (SELECT id, name, age, ROWNUM as TMP_ROW_NUM FROM foo WHERE bar = 1 ORDER BY id ASC) WHERE ROWNUM <= 100";
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		assertEquals(sql, s);
		pagingQueryProvider.setWhereClause("");
		String sql2 = "SELECT * FROM (SELECT id, name, age, ROWNUM as TMP_ROW_NUM FROM foo ORDER BY id ASC) WHERE ROWNUM <= 100";
		String s2 = pagingQueryProvider.generateFirstPageQuery(pageSize);
		assertEquals(sql2, s2);
	}

	@Test @Override
	public void testGenerateRemainingPagesQuery() {
		String sql = "SELECT * FROM (SELECT id, name, age, ROWNUM as TMP_ROW_NUM FROM foo WHERE bar = 1 AND id > ? ORDER BY id ASC) WHERE ROWNUM <= 100";
		String s = pagingQueryProvider.generateRemainingPagesQuery(pageSize);
		assertEquals(sql, s);
	}

	@Test @Override
	public void testGenerateJumpToItemQuery() {
		String sql = "SELECT SORT_KEY FROM (SELECT SORT_KEY, ROWNUM as TMP_ROW_NUM FROM (SELECT id AS SORT_KEY FROM foo WHERE bar = 1 ORDER BY id ASC)) WHERE TMP_ROW_NUM = 100";
		String s = pagingQueryProvider.generateJumpToItemQuery(145, pageSize);
		assertEquals(sql, s);
	}
	
	@Test @Override
	public void testGenerateJumpToItemQueryForFirstPage() {
		String sql = "SELECT SORT_KEY FROM (SELECT SORT_KEY, ROWNUM as TMP_ROW_NUM FROM (SELECT id AS SORT_KEY FROM foo WHERE bar = 1 ORDER BY id ASC)) WHERE TMP_ROW_NUM = 1";
		String s = pagingQueryProvider.generateJumpToItemQuery(45, pageSize);
		assertEquals(sql, s);
	}

	@Override
	@Test
	public void testGenerateFirstPageQueryWithGroupBy() {
		pagingQueryProvider.setGroupClause("dep");
		String sql = "SELECT * FROM (SELECT id, name, age, ROWNUM as TMP_ROW_NUM FROM foo WHERE bar = 1 GROUP BY dep ORDER BY id ASC) WHERE ROWNUM <= 100";
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		assertEquals(sql, s);
	}

	@Override
	@Test
	public void testGenerateRemainingPagesQueryWithGroupBy() {
		pagingQueryProvider.setGroupClause("dep");
		String sql = "SELECT * FROM (SELECT id, name, age, ROWNUM as TMP_ROW_NUM FROM foo WHERE bar = 1 AND id > ? GROUP BY dep ORDER BY id ASC) WHERE ROWNUM <= 100";
		String s = pagingQueryProvider.generateRemainingPagesQuery(pageSize);
		assertEquals(sql, s);
	}

	@Override
	@Test
	public void testGenerateJumpToItemQueryWithGroupBy() {
		pagingQueryProvider.setGroupClause("dep");
		String sql = "SELECT SORT_KEY FROM (SELECT SORT_KEY, ROWNUM as TMP_ROW_NUM FROM (SELECT id AS SORT_KEY FROM foo WHERE bar = 1 GROUP BY dep ORDER BY id ASC)) WHERE TMP_ROW_NUM = 100";
		String s = pagingQueryProvider.generateJumpToItemQuery(145, pageSize);
		assertEquals(sql, s);
	}

	@Override
	@Test
	public void testGenerateJumpToItemQueryForFirstPageWithGroupBy() {
		pagingQueryProvider.setGroupClause("dep");
		String sql = "SELECT SORT_KEY FROM (SELECT SORT_KEY, ROWNUM as TMP_ROW_NUM FROM (SELECT id AS SORT_KEY FROM foo WHERE bar = 1 GROUP BY dep ORDER BY id ASC)) WHERE TMP_ROW_NUM = 1";
		String s = pagingQueryProvider.generateJumpToItemQuery(45, pageSize);
		assertEquals(sql, s);
	}
}
