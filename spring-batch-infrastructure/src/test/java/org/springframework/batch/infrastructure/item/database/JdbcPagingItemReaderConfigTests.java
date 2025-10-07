/*
 * Copyright 2008-2022 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.database.JdbcPagingItemReader;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import org.springframework.test.util.ReflectionTestUtils;

@SpringJUnitConfig
class JdbcPagingItemReaderConfigTests {

	@Autowired
	private JdbcPagingItemReader<Object> jdbcPagingItemReader;

	@Test
	void testConfig() {
		assertNotNull(jdbcPagingItemReader);
		NamedParameterJdbcTemplate namedParameterJdbcTemplate = (NamedParameterJdbcTemplate) ReflectionTestUtils
			.getField(jdbcPagingItemReader, "namedParameterJdbcTemplate");
		JdbcTemplate jdbcTemplate = (JdbcTemplate) namedParameterJdbcTemplate.getJdbcOperations();
		assertEquals(1000, jdbcTemplate.getMaxRows());
		assertEquals(100, jdbcTemplate.getFetchSize());
	}

}
