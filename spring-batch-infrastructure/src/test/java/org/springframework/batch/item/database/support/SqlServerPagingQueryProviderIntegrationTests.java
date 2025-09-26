/*
 * Copyright 2024-2025 the original author or authors.
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

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.junit.jupiter.api.Disabled;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

/**
 * @author Henning Pöttker
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringJUnitConfig
@Sql(scripts = "query-provider-fixture.sql", executionPhase = BEFORE_TEST_CLASS)
@Disabled("https://github.com/spring-projects/spring-batch/issues/4828")
class SqlServerPagingQueryProviderIntegrationTests extends AbstractPagingQueryProviderIntegrationTests {

	// TODO find the best way to externalize and manage image versions
	private static final DockerImageName SQLSERVER_IMAGE = DockerImageName
		.parse("mcr.microsoft.com/mssql/server:2022-CU14-ubuntu-22.04");

	@Container
	public static MSSQLServerContainer<?> sqlserver = new MSSQLServerContainer<>(SQLSERVER_IMAGE).acceptLicense();

	SqlServerPagingQueryProviderIntegrationTests(@Autowired DataSource dataSource) {
		super(dataSource, new SqlServerPagingQueryProvider());
	}

	@Configuration
	static class TestConfiguration {

		@Bean
		public DataSource dataSource() throws Exception {
			SQLServerDataSource dataSource = new SQLServerDataSource();
			dataSource.setUser(sqlserver.getUsername());
			dataSource.setPassword(sqlserver.getPassword());
			dataSource.setURL(sqlserver.getJdbcUrl());
			return dataSource;
		}

	}

}
