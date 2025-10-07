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
package org.springframework.batch.infrastructure.item.database.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.database.Order;
import org.springframework.batch.infrastructure.item.database.support.AbstractSqlPagingQueryProvider;

/**
 * @author Thomas Risberg
 * @author Michael Minella
 * @author Benjamin Hetz
 */
abstract class AbstractSqlPagingQueryProviderTests {

	protected AbstractSqlPagingQueryProvider pagingQueryProvider;

	protected int pageSize;

	@BeforeEach
	void setUp() {
		if (pagingQueryProvider == null) {
			throw new IllegalArgumentException("pagingQueryProvider can't be null");
		}
		pagingQueryProvider.setSelectClause("id, name, age");
		pagingQueryProvider.setFromClause("foo");
		pagingQueryProvider.setWhereClause("bar = 1");

		Map<String, Order> sortKeys = new LinkedHashMap<>();
		sortKeys.put("id", Order.ASCENDING);
		pagingQueryProvider.setSortKeys(sortKeys);
		pageSize = 100;

	}

	@Test
	void testQueryContainsSortKey() {
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize).toLowerCase();
		assertTrue(s.contains("id asc"), "Wrong query: " + s);
	}

	@Test
	void testQueryContainsSortKeyDesc() {
		pagingQueryProvider.getSortKeys().put("id", Order.DESCENDING);
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize).toLowerCase();
		assertTrue(s.contains("id desc"), "Wrong query: " + s);
	}

	@Test
	void testGenerateFirstPageQueryWithMultipleSortKeys() {
		Map<String, Order> sortKeys = new LinkedHashMap<>();
		sortKeys.put("name", Order.ASCENDING);
		sortKeys.put("id", Order.DESCENDING);
		pagingQueryProvider.setSortKeys(sortKeys);
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		assertEquals(getFirstPageSqlWithMultipleSortKeys(), s);
	}

	@Test
	void testGenerateRemainingPagesQueryWithMultipleSortKeys() {
		Map<String, Order> sortKeys = new LinkedHashMap<>();
		sortKeys.put("name", Order.ASCENDING);
		sortKeys.put("id", Order.DESCENDING);
		pagingQueryProvider.setSortKeys(sortKeys);
		String s = pagingQueryProvider.generateRemainingPagesQuery(pageSize);
		assertEquals(getRemainingSqlWithMultipleSortKeys(), s);
	}

	@Test
	void testRemoveKeyWordsFollowedBySpaceChar() {
		String selectClause = "SELECT id, 'yes', false";
		String fromClause = "FROM test.verification_table";
		String whereClause = "WHERE TRUE";
		pagingQueryProvider.setSelectClause(selectClause);
		pagingQueryProvider.setFromClause(fromClause);
		pagingQueryProvider.setWhereClause(whereClause);

		assertEquals("id, 'yes', false", pagingQueryProvider.getSelectClause());
		assertEquals("test.verification_table", pagingQueryProvider.getFromClause());
		assertEquals("TRUE", pagingQueryProvider.getWhereClause());
	}

	@Test
	void testRemoveKeyWordsFollowedByTabChar() {
		String selectClause = "SELECT\tid, 'yes', false";
		String fromClause = "FROM\ttest.verification_table";
		String whereClause = "WHERE\tTRUE";
		pagingQueryProvider.setSelectClause(selectClause);
		pagingQueryProvider.setFromClause(fromClause);
		pagingQueryProvider.setWhereClause(whereClause);

		assertEquals("id, 'yes', false", pagingQueryProvider.getSelectClause());
		assertEquals("test.verification_table", pagingQueryProvider.getFromClause());
		assertEquals("TRUE", pagingQueryProvider.getWhereClause());
	}

	@Test
	void testRemoveKeyWordsFollowedByNewLineChar() {
		String selectClause = "SELECT\nid, 'yes', false";
		String fromClause = "FROM\ntest.verification_table";
		String whereClause = "WHERE\nTRUE";
		pagingQueryProvider.setSelectClause(selectClause);
		pagingQueryProvider.setFromClause(fromClause);
		pagingQueryProvider.setWhereClause(whereClause);

		assertEquals("id, 'yes', false", pagingQueryProvider.getSelectClause());
		assertEquals("test.verification_table", pagingQueryProvider.getFromClause());
		assertEquals("TRUE", pagingQueryProvider.getWhereClause());
	}

	@Test
	abstract void testGenerateFirstPageQuery();

	@Test
	abstract void testGenerateRemainingPagesQuery();

	@Test
	abstract void testGenerateFirstPageQueryWithGroupBy();

	@Test
	abstract void testGenerateRemainingPagesQueryWithGroupBy();

	abstract String getFirstPageSqlWithMultipleSortKeys();

	abstract String getRemainingSqlWithMultipleSortKeys();

}
