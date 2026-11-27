/*
 * Copyright 2026-present the original author or authors.
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

import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.database.Order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Tests the sort key placeholder of {@link AbstractSqlPagingQueryProvider}. The
 * placeholder does not depend on the database dialect, so a single concrete provider is
 * enough and no embedded database is needed: {@code init} only records whether the query
 * uses named parameters.
 *
 * @author Jinhui Kim
 */
class AbstractSqlPagingQueryProviderSortKeyPlaceHolderTests {

	private final Db2PagingQueryProvider pagingQueryProvider = new Db2PagingQueryProvider();

	@BeforeEach
	void setUp() {
		this.pagingQueryProvider.setSelectClause("id, name, age");
		this.pagingQueryProvider.setFromClause("foo");
		this.pagingQueryProvider.setSortKeys(Map.of("id", Order.ASCENDING));
	}

	@Test
	void sortKeyPlaceHolderWithNamedParameters() throws Exception {
		this.pagingQueryProvider.setWhereClause("bar = :bar");
		this.pagingQueryProvider.init(mock(DataSource.class));

		assertEquals(":_id", this.pagingQueryProvider.getSortKeyPlaceHolder("id"));
		assertEquals(":_p.id", this.pagingQueryProvider.getSortKeyPlaceHolder("p.id"));
		// A quoted or qualified column reference is not a valid unquoted parameter name
		assertEquals(":{_\"A\"}", this.pagingQueryProvider.getSortKeyPlaceHolder("\"A\""));
		assertEquals(":{_p.\"A\"}", this.pagingQueryProvider.getSortKeyPlaceHolder("p.\"A\""));
	}

	@Test
	void sortKeyPlaceHolderWithPositionalParameters() throws Exception {
		this.pagingQueryProvider.setWhereClause("bar = ?");
		this.pagingQueryProvider.init(mock(DataSource.class));

		assertEquals("?", this.pagingQueryProvider.getSortKeyPlaceHolder("id"));
		assertEquals("?", this.pagingQueryProvider.getSortKeyPlaceHolder("\"A\""));
	}

}
