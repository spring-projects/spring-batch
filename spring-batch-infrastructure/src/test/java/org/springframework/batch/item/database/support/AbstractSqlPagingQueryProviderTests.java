/*
 * Copyright 2006-2016 the original author or authors.
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
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.database.Order;

/**
 * @author Thomas Risberg
 * @author Michael Minella
 */
public abstract class AbstractSqlPagingQueryProviderTests {

	protected AbstractSqlPagingQueryProvider pagingQueryProvider;
	protected int pageSize;


	@Before
	public void setUp() {
		if (pagingQueryProvider == null) {
			throw new IllegalArgumentException("pagingQueryProvider can't be null");
		}
		pagingQueryProvider.setSelectClause("id, name, age");
		pagingQueryProvider.setFromClause("foo");
		pagingQueryProvider.setWhereClause("bar = 1");
		
		Map<String, Order> sortKeys = new LinkedHashMap<String, Order>();
		sortKeys.put("id", Order.ASCENDING);
		pagingQueryProvider.setSortKeys(sortKeys);
		pageSize = 100;

	}

	@Test
	public void testQueryContainsSortKey(){
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize).toLowerCase();
		assertTrue("Wrong query: "+s, s.contains("id asc"));		
	}

	@Test
	public void testQueryContainsSortKeyDesc(){
		pagingQueryProvider.getSortKeys().put("id", Order.DESCENDING);
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize).toLowerCase();
		assertTrue("Wrong query: "+s, s.contains("id desc"));		
	}

	@Test
	public void testGenerateFirstPageQueryWithMultipleSortKeys() {
		Map<String, Order> sortKeys = new LinkedHashMap<String, Order>();
		sortKeys.put("name", Order.ASCENDING);
		sortKeys.put("id", Order.DESCENDING);
		pagingQueryProvider.setSortKeys(sortKeys);
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		assertEquals(getFirstPageSqlWithMultipleSortKeys(), s);
	}

	@Test
	public void testGenerateRemainingPagesQueryWithMultipleSortKeys() {
		Map<String, Order> sortKeys = new LinkedHashMap<String, Order>();
		sortKeys.put("name", Order.ASCENDING);
		sortKeys.put("id", Order.DESCENDING);
		pagingQueryProvider.setSortKeys(sortKeys);
		String s = pagingQueryProvider.generateRemainingPagesQuery(pageSize);
		assertEquals(getRemainingSqlWithMultipleSortKeys(), s);
	}

	@Test
	public abstract void testGenerateFirstPageQuery();

	@Test
	public abstract void testGenerateRemainingPagesQuery();

	@Test
	public abstract void testGenerateFirstPageQueryWithGroupBy();
	
	@Test
	public abstract void testGenerateRemainingPagesQueryWithGroupBy();
	
	public abstract String getFirstPageSqlWithMultipleSortKeys();
	
	public abstract String getRemainingSqlWithMultipleSortKeys();
	
}
