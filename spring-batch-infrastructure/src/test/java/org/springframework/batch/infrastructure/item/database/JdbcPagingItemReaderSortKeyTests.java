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
package org.springframework.batch.infrastructure.item.database;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.database.support.Db2PagingQueryProvider;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Mahmoud Ben Hassine
 */
class JdbcPagingItemReaderSortKeyTests {

	private static final String FROM = "records p JOIN related r ON r.id = p.id + 100";

	private EmbeddedDatabase dataSource;

	@BeforeEach
	void setUp() {
		this.dataSource = new EmbeddedDatabaseBuilder().generateUniqueName(true)
			.setType(EmbeddedDatabaseType.H2)
			.build();
		JdbcTemplate jdbcTemplate = new JdbcTemplate(this.dataSource);
		jdbcTemplate.execute("SET MODE DB2");
		jdbcTemplate.execute("CREATE TABLE records (id BIGINT PRIMARY KEY, group_id INTEGER)");
		jdbcTemplate.execute("CREATE TABLE related (id BIGINT PRIMARY KEY)");
		for (long id = 1; id <= 5; id++) {
			jdbcTemplate.update("INSERT INTO records VALUES (?, ?)", id, (id - 1) / 2);
			jdbcTemplate.update("INSERT INTO related VALUES (?)", id + 100);
		}
	}

	@AfterEach
	void tearDown() {
		this.dataSource.shutdown();
	}

	@Test
	void readWithQualifiedSortKey() throws Exception {
		JdbcPagingItemReader<Long> reader = reader("p.id", FROM, Map.of("p.id", Order.ASCENDING), false);

		assertEquals(List.of(1L, 2L, 3L, 4L, 5L), readAll(reader, new ExecutionContext()));
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void restartWithQualifiedSortKey(boolean namedParameters) throws Exception {
		assertRestart("p.id", Map.of("p.id", Order.ASCENDING), namedParameters, List.of(1L, 2L), List.of(3L, 4L, 5L),
				Map.of("p.id", 2L));
	}

	@Test
	void restartWithinPageWithQualifiedSortKey() throws Exception {
		assertRestart("p.id", Map.of("p.id", Order.ASCENDING), true, List.of(1L, 2L, 3L), List.of(4L, 5L),
				Map.of("p.id", 2L));
	}

	@Test
	void readWithExplicitQualifiedOutputAlias() throws Exception {
		JdbcPagingItemReader<Long> reader = reader("p.id AS \"p.id\"", FROM, Map.of("p.id", Order.ASCENDING), false);

		assertEquals(List.of(1L, 2L, 3L, 4L, 5L), readAll(reader, new ExecutionContext()));
	}

	@Test
	void preferQualifiedOutputAliasOverDifferentUnqualifiedColumn() throws Exception {
		JdbcPagingItemReader<Long> reader = reader("p.id + 1000 AS id, p.id AS \"p.id\"", FROM,
				Map.of("p.id", Order.ASCENDING), false);
		reader.setRowMapper((rs, rowNum) -> rs.getLong(2));

		assertEquals(List.of(1L, 2L, 3L, 4L, 5L), readAll(reader, new ExecutionContext()));
	}

	@Test
	void restartWithCompositeSortKeys() throws Exception {
		Map<String, Order> sortKeys = new LinkedHashMap<>();
		sortKeys.put("p.group_id", Order.ASCENDING);
		sortKeys.put("p.id", Order.DESCENDING);

		assertRestart("p.id, p.group_id", sortKeys, true, List.of(2L, 1L), List.of(4L, 3L, 5L),
				Map.of("p.group_id", 0, "p.id", 1L));
	}

	@Test
	void readWithSchemaQualifiedSortKey() throws Exception {
		JdbcPagingItemReader<Long> reader = reader("id", "PUBLIC.records", Map.of("PUBLIC.records.id", Order.ASCENDING),
				false);

		assertEquals(List.of(1L, 2L, 3L, 4L, 5L), readAll(reader, new ExecutionContext()));
	}

	@Test
	void rejectAmbiguousOutputLabels() throws Exception {
		JdbcPagingItemReader<Long> reader = reader("p.id, r.id", FROM, Map.of("r.id", Order.ASCENDING), false);
		try {
			reader.open(new ExecutionContext());
			assertThrows(BadSqlGrammarException.class, reader::read);
		}
		finally {
			reader.close();
		}
	}

	@Test
	void rejectMissingUnqualifiedOutputLabel() throws Exception {
		JdbcPagingItemReader<Long> reader = reader("p.group_id", "records p", Map.of("id", Order.ASCENDING), false);
		try {
			reader.open(new ExecutionContext());
			assertThrows(BadSqlGrammarException.class, reader::read);
		}
		finally {
			reader.close();
		}
	}

	private void assertRestart(String selectClause, Map<String, Order> sortKeys, boolean namedParameters,
			List<Long> firstItems, List<Long> remainingItems, Map<String, Object> expectedKeys) throws Exception {
		ExecutionContext executionContext = new ExecutionContext();
		JdbcPagingItemReader<Long> reader = reader(selectClause, FROM, sortKeys, namedParameters);
		try {
			reader.open(executionContext);
			for (Long item : firstItems) {
				assertEquals(item, reader.read());
			}
			reader.update(executionContext);
			assertEquals(expectedKeys, executionContext.get("reader.start.after"));
		}
		finally {
			reader.close();
		}

		JdbcPagingItemReader<Long> restartedReader = reader(selectClause, FROM, sortKeys, namedParameters);
		assertEquals(remainingItems, readAll(restartedReader, executionContext));
	}

	private JdbcPagingItemReader<Long> reader(String selectClause, String fromClause, Map<String, Order> sortKeys,
			boolean namedParameters) throws Exception {
		Db2PagingQueryProvider queryProvider = new Db2PagingQueryProvider();
		queryProvider.setSelectClause(selectClause);
		queryProvider.setFromClause(fromClause);
		queryProvider.setWhereClause(namedParameters ? "1 = :enabled" : "1 = ?");
		queryProvider.setSortKeys(sortKeys);
		JdbcPagingItemReader<Long> reader = new JdbcPagingItemReader<>(this.dataSource, queryProvider);
		reader.setName("reader");
		reader.setPageSize(2);
		reader.setParameterValues(Map.of(namedParameters ? "enabled" : "0", 1));
		reader.setRowMapper((rs, rowNum) -> rs.getLong(1));
		reader.afterPropertiesSet();
		return reader;
	}

	private List<Long> readAll(JdbcPagingItemReader<Long> reader, ExecutionContext executionContext) throws Exception {
		try {
			reader.open(executionContext);
			List<Long> items = new ArrayList<>();
			Long item;
			while ((item = reader.read()) != null) {
				items.add(item);
			}
			return items;
		}
		finally {
			reader.close();
		}
	}

}
