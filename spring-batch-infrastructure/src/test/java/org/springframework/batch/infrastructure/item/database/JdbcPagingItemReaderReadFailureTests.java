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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link JdbcPagingItemReader} reading again after a failed page query, as a
 * retry of {@link JdbcPagingItemReader#read()} does.
 *
 * @author WooSeung Kim
 */
@SpringJUnitConfig(locations = "classpath:data-source-context.xml")
class JdbcPagingItemReaderReadFailureTests {

	@Autowired
	private DataSource dataSource;

	@Test
	void readAfterFailedFirstPageQueryReadsFirstPageAgain() throws Exception {
		AtomicBoolean fail = new AtomicBoolean(false);
		DataSource dataSource = new DelegatingDataSource(this.dataSource) {
			@Override
			public Connection getConnection() throws SQLException {
				if (fail.getAndSet(false)) {
					throw new SQLTransientConnectionException("Connection is not available");
				}
				return super.getConnection();
			}
		};
		JdbcPagingItemReader<Integer> reader = createItemReader(dataSource, (rs, rowNum) -> rs.getInt("ID"));
		fail.set(true);

		assertThrows(DataAccessException.class, reader::read);
		assertEquals(List.of(1, 2, 3, 4, 5), readAll(reader));
	}

	@Test
	void readAfterFailureWhileMappingPageReadsPageAgainFromSamePosition() throws Exception {
		AtomicBoolean fail = new AtomicBoolean(true);
		RowMapper<Integer> rowMapper = (rs, rowNum) -> {
			int id = rs.getInt("ID");
			if (id == 4 && fail.getAndSet(false)) {
				throw new SQLTransientConnectionException("Connection reset");
			}
			return id;
		};
		JdbcPagingItemReader<Integer> reader = createItemReader(this.dataSource, rowMapper);

		assertEquals(1, reader.read());
		assertEquals(2, reader.read());
		assertThrows(DataAccessException.class, reader::read);
		assertEquals(List.of(3, 4, 5), readAll(reader));
	}

	private JdbcPagingItemReader<Integer> createItemReader(DataSource dataSource, RowMapper<Integer> rowMapper)
			throws Exception {
		JdbcPagingItemReader<Integer> reader = new JdbcPagingItemReaderBuilder<Integer>().name("fooReader")
			.dataSource(dataSource)
			.selectClause("select ID")
			.fromClause("from T_FOOS")
			.sortKeys(Map.of("ID", Order.ASCENDING))
			.rowMapper(rowMapper)
			.pageSize(2)
			.build();
		reader.open(new ExecutionContext());
		return reader;
	}

	private List<Integer> readAll(JdbcPagingItemReader<Integer> reader) throws Exception {
		List<Integer> items = new ArrayList<>();
		Integer item;
		while ((item = reader.read()) != null) {
			items.add(item);
		}
		return items;
	}

}
