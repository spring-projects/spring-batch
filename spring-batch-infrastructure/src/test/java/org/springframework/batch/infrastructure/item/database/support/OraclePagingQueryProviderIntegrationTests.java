/*
 * Copyright 2024-2026 the original author or authors.
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
package org.springframework.batch.infrastructure.item.database.support;

import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

import javax.sql.DataSource;

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

import oracle.jdbc.pool.OracleDataSource;

/**
 * Official Docker images for Oracle are not publicly available. Oracle support is tested
 * semi-manually for the moment: 1.
 *
 * @author Henning Pöttker
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringJUnitConfig
@Sql(scripts = "query-provider-fixture-oracle.sql", executionPhase = BEFORE_TEST_CLASS)
@Disabled("Official Docker images for Oracle are not publicly available")
class OraclePagingQueryProviderIntegrationTests extends AbstractPagingQueryProviderIntegrationTests {

	private static final DockerImageName ORACLE_IMAGE = DockerImageName.parse("gvenzl/oracle-xe");

	@Container
	public static OracleContainer oracle = new OracleContainer(ORACLE_IMAGE);

	OraclePagingQueryProviderIntegrationTests(@Autowired DataSource dataSource) {
		super(dataSource, new OraclePagingQueryProvider());
	}

	@Configuration
	static class TestConfiguration {

		@Bean
		public DataSource dataSource() throws Exception {
			OracleDataSource datasource = new OracleDataSource();
			datasource.setURL(oracle.getJdbcUrl());
			datasource.setUser(oracle.getUsername());
			datasource.setPassword(oracle.getPassword());
			return datasource;
		}

	}

}
