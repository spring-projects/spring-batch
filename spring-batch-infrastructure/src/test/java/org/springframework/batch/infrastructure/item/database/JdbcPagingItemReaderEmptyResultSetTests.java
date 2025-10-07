/*
 * Copyright 2021-2022 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.database.JdbcPagingItemReader;
import org.springframework.batch.infrastructure.item.database.Order;
import org.springframework.batch.infrastructure.item.database.support.HsqlPagingQueryProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(locations = "JdbcPagingItemReaderCommonTests-context.xml")
class JdbcPagingItemReaderEmptyResultSetTests {

	private static final int PAGE_SIZE = 2;

	private static final int EMPTY_READS = PAGE_SIZE + 1;

	@Autowired
	private DataSource dataSource;

	@Test
	void testMultiplePageReadsOnEmptyResultSet() throws Exception {
		final ItemReader<Long> reader = getItemReader();
		for (int i = 0; i < EMPTY_READS; i++) {
			assertNull(reader.read());
		}
	}

	private ItemReader<Long> getItemReader() throws Exception {
		HsqlPagingQueryProvider queryProvider = new HsqlPagingQueryProvider();
		queryProvider.setSelectClause("select ID");
		queryProvider.setFromClause("from T_FOOS");
		queryProvider.setWhereClause("1 = 0");
		queryProvider.setSortKeys(Collections.singletonMap("ID", Order.ASCENDING));
		JdbcPagingItemReader<Long> reader = new JdbcPagingItemReader<>(dataSource, queryProvider);
		reader.setRowMapper(new SingleColumnRowMapper<>());
		reader.setPageSize(PAGE_SIZE);
		reader.afterPropertiesSet();
		reader.setSaveState(false);

		return reader;
	}

}
