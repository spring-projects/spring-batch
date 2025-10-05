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

package org.springframework.batch.infrastructure.support;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.support.DatabaseType;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class DatabaseTypeIntegrationTests {

	@Test
	void testH2() throws Exception {
		DataSource dataSource = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
			.generateUniqueName(true)
			.build();
		assertEquals(DatabaseType.H2, DatabaseType.fromMetaData(dataSource));
		dataSource.getConnection();
	}

	@Test
	void testDerby() throws Exception {
		DataSource dataSource = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.DERBY)
			.generateUniqueName(true)
			.build();
		assertEquals(DatabaseType.DERBY, DatabaseType.fromMetaData(dataSource));
		dataSource.getConnection();
	}

}
