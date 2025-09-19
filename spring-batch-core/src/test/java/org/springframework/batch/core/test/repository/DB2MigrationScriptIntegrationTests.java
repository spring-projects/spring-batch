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

import com.ibm.db2.jcc.DB2SimpleDataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration tests for DB2 migration script for v5.0
 *
 * @author Jinwoo Bae
 */
@Testcontainers(disabledWithoutDocker = true)
@Disabled("DB2 migration script has MODIFY COLUMN syntax issues - enable when migration script is fixed")
class DB2MigrationScriptIntegrationTests {

	private static final DockerImageName DB2_IMAGE = DockerImageName.parse("icr.io/db2_community/db2:12.1.0.0");

	@Container
	public static Db2Container db2 = new Db2Container(DB2_IMAGE).acceptLicense();

	@Test
	void db2SchemaShouldBeValid() {
		DB2SimpleDataSource datasource = createDataSource();
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();

		databasePopulator.addScript(new ClassPathResource("/org/springframework/batch/core/schema-db2-v4.sql"));
		databasePopulator
			.addScript(new ClassPathResource("/org/springframework/batch/core/migration/5.0/migration-db2.sql"));

		Assertions.assertDoesNotThrow(() -> databasePopulator.execute(datasource));
	}

	private DB2SimpleDataSource createDataSource() {
		DB2SimpleDataSource dataSource = new DB2SimpleDataSource();
		dataSource.setDatabaseName(db2.getDatabaseName());
		dataSource.setUser(db2.getUsername());
		dataSource.setPassword(db2.getPassword());
		dataSource.setDriverType(4);
		dataSource.setServerName(db2.getHost());
		dataSource.setPortNumber(db2.getMappedPort(Db2Container.DB2_PORT));
		dataSource.setSslConnection(false);
		return dataSource;
	}

}
