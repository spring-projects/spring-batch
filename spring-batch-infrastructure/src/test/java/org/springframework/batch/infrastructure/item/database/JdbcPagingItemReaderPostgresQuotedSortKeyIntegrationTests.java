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
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.database.support.PostgresPagingQueryProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Sort keys that are quoted column references, as required for upper case or mixed case
 * column names in PostgreSQL. PostgreSQL folds unquoted identifiers to lower case, so a
 * column named {@code "A"} must be referenced and sorted with quotes, while the result
 * set column label stays {@code A}.
 *
 * @author Jinhui Kim
 */
@Testcontainers(disabledWithoutDocker = true)
class JdbcPagingItemReaderPostgresQuotedSortKeyIntegrationTests {

	private static final DockerImageName POSTGRESQL_IMAGE = DockerImageName.parse("postgres:17.5");

	@Container
	public static PostgreSQLContainer postgres = new PostgreSQLContainer(POSTGRESQL_IMAGE);

	private PGSimpleDataSource dataSource;

	@BeforeEach
	void setUp() {
		this.dataSource = new PGSimpleDataSource();
		this.dataSource.setURL(postgres.getJdbcUrl());
		this.dataSource.setUser(postgres.getUsername());
		this.dataSource.setPassword(postgres.getPassword());
		JdbcTemplate jdbcTemplate = new JdbcTemplate(this.dataSource);
		jdbcTemplate.execute("DROP TABLE IF EXISTS records");
		jdbcTemplate.execute("CREATE TABLE records (\"A\" BIGINT PRIMARY KEY, \"group\" INTEGER)");
		for (long id = 1; id <= 5; id++) {
			jdbcTemplate.update("INSERT INTO records VALUES (?, ?)", id, (id - 1) / 2);
		}
	}

	@Test
	void readWithQuotedSortKeyAndNamedParameters() throws Exception {
		assertEquals(List.of(1L, 2L, 3L, 4L, 5L), readAll("\"A\", \"group\"", Map.of("\"A\"", Order.ASCENDING), true));
	}

	@Test
	void restartWithQuotedSortKey() throws Exception {
		ExecutionContext executionContext = new ExecutionContext();
		JdbcPagingItemReader<Long> reader = reader("\"A\", \"group\"", Map.of("\"A\"", Order.ASCENDING), false);
		try {
			reader.open(executionContext);
			assertEquals(1L, reader.read());
			assertEquals(2L, reader.read());
			reader.update(executionContext);
			assertEquals(Map.of("\"A\"", 2L), executionContext.get("reader.start.after"));
		}
		finally {
			reader.close();
		}

		JdbcPagingItemReader<Long> restartedReader = reader("\"A\", \"group\"", Map.of("\"A\"", Order.ASCENDING),
				false);
		assertEquals(List.of(3L, 4L, 5L), readAll(restartedReader, executionContext));
	}

	private List<Long> readAll(String selectClause, Map<String, Order> sortKeys, boolean namedParameters)
			throws Exception {
		return readAll(reader(selectClause, sortKeys, namedParameters), new ExecutionContext());
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

	private JdbcPagingItemReader<Long> reader(String selectClause, Map<String, Order> sortKeys, boolean namedParameters)
			throws Exception {
		PostgresPagingQueryProvider queryProvider = new PostgresPagingQueryProvider();
		queryProvider.setSelectClause(selectClause);
		queryProvider.setFromClause("records p");
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

}
