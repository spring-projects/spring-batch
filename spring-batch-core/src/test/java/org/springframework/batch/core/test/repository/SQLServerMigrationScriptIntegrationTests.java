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

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration tests for SQLServer migration script for v5.0
 *
 * @author Jinwoo Bae
 */
@Testcontainers(disabledWithoutDocker = true)
class SQLServerMigrationScriptIntegrationTests {

	@Container
	public static MSSQLServerContainer<?> sqlserver = new MSSQLServerContainer<>(
			DockerImageName.parse("mcr.microsoft.com/mssql/server:2019-latest"))
		.acceptLicense();

	@Test
	void migrationScriptShouldBeValid() {
		SQLServerDataSource datasource = createDataSource();
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		databasePopulator.addScript(new ClassPathResource("/org/springframework/batch/core/schema-sqlserver-v4.sql"));
		databasePopulator
			.addScript(new ClassPathResource("/org/springframework/batch/core/migration/5.0/migration-sqlserver.sql"));

		Assertions.assertDoesNotThrow(() -> databasePopulator.execute(datasource));
	}

	private SQLServerDataSource createDataSource() {
		SQLServerDataSource datasource = new SQLServerDataSource();
		datasource.setURL(sqlserver.getJdbcUrl());
		datasource.setUser(sqlserver.getUsername());
		datasource.setPassword(sqlserver.getPassword());
		return datasource;
	}

}
