/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.batch.item.database.support;

import javax.sql.DataSource;

import oracle.jdbc.pool.OracleDataSource;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

/**
 * Official Docker images for Oracle are not publicly available. Oracle support is tested
 * semi-manually for the moment: 1. Build a docker image for gvenzl/oracle-free:23.7:
 * <a href=
 * "https://github.com/oracle/docker-images/tree/main/OracleDatabase/SingleInstance#running-oracle-database-11gr2-express-edition-in-a-container">...</a>
 * 2. Run the test `testJobExecution`
 *
 * @author Henning Pöttker
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringJUnitConfig
@Sql(scripts = "query-provider-fixture.sql", executionPhase = BEFORE_TEST_CLASS)
@Disabled("Official Docker images for Oracle are not publicly available")
class OraclePagingQueryProviderIntegrationTests extends AbstractPagingQueryProviderIntegrationTests {

	// TODO find the best way to externalize and manage image versions
	private static final DockerImageName ORACLE_IMAGE = DockerImageName.parse("gvenzl/oracle-free:23.7");

	@Container
	public static OracleContainer oracle = new OracleContainer(ORACLE_IMAGE);

	OraclePagingQueryProviderIntegrationTests(@Autowired DataSource dataSource) {
		super(dataSource, new OraclePagingQueryProvider());
	}

	@Configuration
	static class TestConfiguration {

		@Bean
		public DataSource dataSource() throws Exception {
			OracleDataSource oracleDataSource = new OracleDataSource();
			oracleDataSource.setUser(oracle.getUsername());
			oracleDataSource.setPassword(oracle.getPassword());
			oracleDataSource.setDatabaseName(oracle.getDatabaseName());
			oracleDataSource.setServerName(oracle.getHost());
			oracleDataSource.setPortNumber(oracle.getOraclePort());
			return oracleDataSource;
		}

	}

}
