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

import org.junit.Before;
import org.junit.Test;

/**
 * @author Thomas Risberg
 */
public abstract class AbstractSqlPagingQueryProviderTests {

	protected AbstractSqlPagingQueryProvider pagingQueryProvider;
	protected int pageSize;


	@Before
	public void setUp() {
		if (pagingQueryProvider == null) {
			throw new IllegalArgumentException("pagingQuery{rovider can't be null");
		}
		pagingQueryProvider.setSelectClause("id, name, age");
		pagingQueryProvider.setFromClause("foo");
		pagingQueryProvider.setWhereClause("bar = 1");
		pagingQueryProvider.setSortKey("id");
		pageSize = 100;

	}

	@Test
	public abstract void testGenerateFirstPageQuery();

	@Test
	public abstract void testGenerateRemainingPagesQuery();

	@Test
	public abstract void testGenerateJumpToItemQuery();
	
}
