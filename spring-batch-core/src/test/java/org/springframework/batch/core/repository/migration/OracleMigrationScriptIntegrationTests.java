/*
 * Copyright 2025-present the original author or authors.
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
package org.springframework.batch.core.repository.migration;

import oracle.jdbc.pool.OracleDataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration tests for Oracle migration script for v6.0
 *
 * @author Jinwoo Bae
 * @author Mahmoud Ben Hassine
 */
@Disabled("On purpose, not part of the CI build. Used on demand to validate migration scripts.")
@Testcontainers(disabledWithoutDocker = true)
class OracleMigrationScriptIntegrationTests {

	@Container
	public static OracleContainer oracle = new OracleContainer(DockerImageName.parse("gvenzl/oracle-xe"));

	@Test
	void migrationScriptShouldBeValid() throws Exception {
		OracleDataSource datasource = createDataSource();

		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		databasePopulator.addScript(new FileSystemResource(
				"src/test/resources/org/springframework/batch/core/repository/migration/schema-oracle-v5.2.sql"));
		databasePopulator.addScript(new FileSystemResource(
				"src/main/resources/org/springframework/batch/core/migration/6.0/migration-oracle.sql"));
		Assertions.assertDoesNotThrow(() -> databasePopulator.execute(datasource));
	}

	private OracleDataSource createDataSource() throws Exception {
		OracleDataSource datasource = new OracleDataSource();
		datasource.setURL(oracle.getJdbcUrl());
		datasource.setUser(oracle.getUsername());
		datasource.setPassword(oracle.getPassword());
		return datasource;
	}

}
