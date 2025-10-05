/*
 * Copyright 2006-2023 the original author or authors.
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

package org.springframework.batch.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.Test;

@SpringJUnitConfig(locations = "/org/springframework/batch/infrastructure/jms/jms-context.xml")
class DatasourceTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Transactional
	@Test
	void testTemplate() {
		JdbcTestUtils.deleteFromTables(jdbcTemplate, "T_BARS");
		int count = JdbcTestUtils.countRowsInTable(jdbcTemplate, "T_BARS");
		assertEquals(0, count);

		jdbcTemplate.update("INSERT into T_BARS (id,name,foo_date) values (?,?,null)", 0, "foo");
	}

}
