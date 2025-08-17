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

import net.sourceforge.jtds.jdbcx.JtdsDataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration tests for Sybase migration script for v5.0
 *
 * @author Jinwoo Bae
 */
@Testcontainers(disabledWithoutDocker = true)
class SybaseMigrationScriptIntegrationTests {

	@Container
	public static GenericContainer<?> sybase = new GenericContainer<>(DockerImageName.parse("datagrip/sybase:16.0"))
		.withExposedPorts(5000)
		.withEnv("SYBASE_PASSWORD", "myPassword");

	// Note: This test currently FAILS due to Sybase-specific database configuration
	// issues:
	// - Sybase requires 'select into' or 'full logging for alter table' options to be
	// enabled
	// - Error: "Neither the 'select into' nor the 'full logging for alter table' database
	// options are enabled for database 'master'. ALTER TABLE with data copy cannot be
	// done."
	@Test
	@Disabled
	void migrationScriptShouldBeValid() {
		JtdsDataSource datasource = createDataSource();
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();

		databasePopulator.addScript(new ClassPathResource("/org/springframework/batch/core/schema-sybase-4.sql"));
		databasePopulator
			.addScript(new ClassPathResource("/org/springframework/batch/core/migration/5.0/migration-sybase.sql"));

		Assertions.assertDoesNotThrow(() -> databasePopulator.execute(datasource));
	}

	private JtdsDataSource createDataSource() {
		JtdsDataSource datasource = new JtdsDataSource();
		datasource.setServerType(2);
		datasource.setServerName(sybase.getHost());
		datasource.setPortNumber(sybase.getMappedPort(5000));
		datasource.setDatabaseName("master");
		datasource.setUser("sa");
		datasource.setPassword("myPassword");
		return datasource;
	}

}
