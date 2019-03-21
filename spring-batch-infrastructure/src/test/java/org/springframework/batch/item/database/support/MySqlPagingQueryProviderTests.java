/*
 * Copyright 2006-2015 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.batch.item.database.Order;

import static org.junit.Assert.assertEquals;

/**
 * @author Thomas Risberg
 * @author Michael Minella
 */
public class MySqlPagingQueryProviderTests extends AbstractSqlPagingQueryProviderTests {

	public MySqlPagingQueryProviderTests() {
		pagingQueryProvider = new MySqlPagingQueryProvider();
	}

	@Test
	@Override
	public void testGenerateFirstPageQuery() {
		String sql = "SELECT id, name, age FROM foo WHERE bar = 1 ORDER BY id ASC LIMIT 100";
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		assertEquals(sql, s);
	}

	@Test @Override
	public void testGenerateRemainingPagesQuery() {
		String sql = "SELECT id, name, age FROM foo WHERE (bar = 1) AND ((id > ?)) ORDER BY id ASC LIMIT 100";
		String s = pagingQueryProvider.generateRemainingPagesQuery(pageSize);
		assertEquals(sql, s);
	}

	@Test @Override
	public void testGenerateJumpToItemQuery() {
		String sql = "SELECT id FROM foo WHERE bar = 1 ORDER BY id ASC LIMIT 99, 1";
		String s = pagingQueryProvider.generateJumpToItemQuery(145, pageSize);
		assertEquals(sql, s);
	}
	
	@Test @Override
	public void testGenerateJumpToItemQueryForFirstPage() {
		String sql = "SELECT id FROM foo WHERE bar = 1 ORDER BY id ASC LIMIT 0, 1";
		String s = pagingQueryProvider.generateJumpToItemQuery(45, pageSize);
		assertEquals(sql, s);
	}

	@Override
	@Test
	public void testGenerateFirstPageQueryWithGroupBy() {
		pagingQueryProvider.setGroupClause("dep");
		String sql = "SELECT id, name, age FROM foo WHERE bar = 1 GROUP BY dep ORDER BY id ASC LIMIT 100";
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		assertEquals(sql, s);
	}

	@Override
	@Test
	public void testGenerateRemainingPagesQueryWithGroupBy() {
		pagingQueryProvider.setGroupClause("dep");
		String sql = "SELECT *  FROM (SELECT id, name, age FROM foo WHERE bar = 1 GROUP BY dep) AS MAIN_QRY WHERE ((id > ?)) ORDER BY id ASC LIMIT 100";
		String s = pagingQueryProvider.generateRemainingPagesQuery(pageSize);
		assertEquals(sql, s);
	}

	@Override
	@Test
	public void testGenerateJumpToItemQueryWithGroupBy() {
		pagingQueryProvider.setGroupClause("dep");
		String sql = "SELECT id FROM foo WHERE bar = 1 GROUP BY dep ORDER BY id ASC LIMIT 99, 1";
		String s = pagingQueryProvider.generateJumpToItemQuery(145, pageSize);
		assertEquals(sql, s);
	}

	@Override
	@Test
	public void testGenerateJumpToItemQueryForFirstPageWithGroupBy() {
		pagingQueryProvider.setGroupClause("dep");
		String sql = "SELECT id FROM foo WHERE bar = 1 GROUP BY dep ORDER BY id ASC LIMIT 0, 1";
		String s = pagingQueryProvider.generateJumpToItemQuery(45, pageSize);
		assertEquals(sql, s);
	}

	@Test
	public void testFirstPageSqlWithAliases() {
		Map<String, Order> sorts = new HashMap<>();
		sorts.put("owner.id", Order.ASCENDING);

		this.pagingQueryProvider = new MySqlPagingQueryProvider();
		this.pagingQueryProvider.setSelectClause("SELECT owner.id as ownerid, first_name, last_name, dog_name ");
		this.pagingQueryProvider.setFromClause("FROM dog_owner owner INNER JOIN dog ON owner.id = dog.id ");
		this.pagingQueryProvider.setSortKeys(sorts);

		String firstPage = this.pagingQueryProvider.generateFirstPageQuery(5);
		String jumpToItemQuery = this.pagingQueryProvider.generateJumpToItemQuery(7, 5);
		String remainingPagesQuery = this.pagingQueryProvider.generateRemainingPagesQuery(5);

		assertEquals("SELECT owner.id as ownerid, first_name, last_name, dog_name FROM dog_owner owner INNER JOIN dog ON owner.id = dog.id ORDER BY owner.id ASC LIMIT 5", firstPage);
		assertEquals("SELECT owner.id FROM dog_owner owner INNER JOIN dog ON owner.id = dog.id ORDER BY owner.id ASC LIMIT 4, 1", jumpToItemQuery);
		assertEquals("SELECT owner.id as ownerid, first_name, last_name, dog_name FROM dog_owner owner INNER JOIN dog ON owner.id = dog.id WHERE ((owner.id > ?)) ORDER BY owner.id ASC LIMIT 5", remainingPagesQuery);
	}

	@Override
	public String getFirstPageSqlWithMultipleSortKeys() {
		return "SELECT id, name, age FROM foo WHERE bar = 1 ORDER BY name ASC, id DESC LIMIT 100";
	}

	@Override
	public String getRemainingSqlWithMultipleSortKeys() {
		return "SELECT id, name, age FROM foo WHERE (bar = 1) AND ((name > ?) OR (name = ? AND id < ?)) ORDER BY name ASC, id DESC LIMIT 100";
	}

	@Override
	public String getJumpToItemQueryWithMultipleSortKeys() {
		return "SELECT name, id FROM foo WHERE bar = 1 ORDER BY name ASC, id DESC LIMIT 99, 1";
	}

	@Override
	public String getJumpToItemQueryForFirstPageWithMultipleSortKeys() {
		return "SELECT name, id FROM foo WHERE bar = 1 ORDER BY name ASC, id DESC LIMIT 0, 1";
	}
}
