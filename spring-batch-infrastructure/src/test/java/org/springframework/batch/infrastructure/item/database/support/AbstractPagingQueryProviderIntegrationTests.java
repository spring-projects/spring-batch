/*
 * Copyright 2024 the original author or authors.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.database.Order;
import org.springframework.batch.infrastructure.item.database.support.AbstractSqlPagingQueryProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Henning Pöttker
 */
abstract class AbstractPagingQueryProviderIntegrationTests {

	private final JdbcTemplate jdbcTemplate;

	private final AbstractSqlPagingQueryProvider queryProvider;

	AbstractPagingQueryProviderIntegrationTests(DataSource dataSource, AbstractSqlPagingQueryProvider queryProvider) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		this.queryProvider = queryProvider;
	}

	@Test
	void testWithoutGrouping() {
		queryProvider.setSelectClause("ID, STRING");
		queryProvider.setFromClause("TEST_TABLE");
		Map<String, Order> sortKeys = new HashMap<>();
		sortKeys.put("ID", Order.ASCENDING);
		queryProvider.setSortKeys(sortKeys);

		List<Item> firstPage = jdbcTemplate.query(queryProvider.generateFirstPageQuery(2), MAPPER);
		assertEquals(List.of(new Item(1, "Spring"), new Item(2, "Batch")), firstPage);

		List<Item> secondPage = jdbcTemplate.query(queryProvider.generateRemainingPagesQuery(2), MAPPER, 2);
		assertEquals(List.of(new Item(3, "Infrastructure")), secondPage);
	}

	@Test
	void testWithGrouping() {
		queryProvider.setSelectClause("STRING");
		queryProvider.setFromClause("GROUPING_TEST_TABLE");
		queryProvider.setGroupClause("STRING");
		Map<String, Order> sortKeys = new HashMap<>();
		sortKeys.put("STRING", Order.ASCENDING);
		queryProvider.setSortKeys(sortKeys);

		List<String> firstPage = jdbcTemplate.queryForList(queryProvider.generateFirstPageQuery(2), String.class);
		assertEquals(List.of("Batch", "Infrastructure"), firstPage);

		List<String> secondPage = jdbcTemplate.queryForList(queryProvider.generateRemainingPagesQuery(2), String.class,
				"Infrastructure");
		assertEquals(List.of("Spring"), secondPage);
	}

	private record Item(Integer id, String string) {
	}

	private static final RowMapper<Item> MAPPER = (rs, rowNum) -> new Item(rs.getInt("id"), rs.getString("string"));

}
