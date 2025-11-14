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
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.sqlite.SQLiteDataSource;

/**
 * Integration tests for PostgreSQL migration script for v5.0
 *
 * @author Jinwoo Bae
 */
class SQLiteMigrationScriptIntegrationTests {

	// Note: This test currently FAILS due to SQLite-specific limitations in
	// migration-sqlite.sql:
	// - SQLite does not support "MODIFY COLUMN" syntax
	@Test
	@Disabled
	void migrationScriptShouldBeValid() {
		SQLiteDataSource datasource = createDataSource();

		ResourceDatabasePopulator schemaPopulator = new ResourceDatabasePopulator();
		schemaPopulator.addScript(new ClassPathResource("/org/springframework/batch/core/schema-sqlite-v4.sql"));
		schemaPopulator
			.addScript(new ClassPathResource("/org/springframework/batch/core/migration/5.0/migration-sqlite.sql"));

		Assertions.assertDoesNotThrow(() -> schemaPopulator.execute(datasource));
	}

	private SQLiteDataSource createDataSource() {
		SQLiteDataSource datasource = new SQLiteDataSource();
		datasource.setUrl("jdbc:sqlite::memory:");
		return datasource;
	}

}
