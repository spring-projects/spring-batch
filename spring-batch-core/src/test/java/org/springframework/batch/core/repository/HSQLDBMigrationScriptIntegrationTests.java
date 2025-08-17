/*
 * Copyright 2020-2025 the original author or authors.
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
package org.springframework.batch.core.test.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * Integration tests for HSQLDB migration script for v5.0
 *
 * @author Jinwoo Bae
 */
class HSQLDBMigrationScriptIntegrationTests {

	// Note: This test currently FAILS due to HSQLDB-specific syntax issues in
	// migration-hsqldb.sql:
	// - HSQLDB does not support "MODIFY COLUMN"
	@Test
	@Disabled
	void migrationScriptShouldBeValid() {
		EmbeddedDatabase datasource = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL)
			.addScript("/org/springframework/batch/core/schema-hsqldb-v4.sql")
			.build();

		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		databasePopulator
			.addScript(new ClassPathResource("/org/springframework/batch/core/migration/5.0/migration-hsqldb.sql"));

		Assertions.assertDoesNotThrow(() -> databasePopulator.execute(datasource));
	}

}
